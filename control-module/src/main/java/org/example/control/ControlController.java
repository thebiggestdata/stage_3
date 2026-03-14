package org.example.control;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.shared.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for the Control Module (orchestrator).
 *
 * Endpoints:
 *   GET  /api/health                                      – liveness probe
 *   GET  /api/cluster/status                              – cluster member count + index stats
 *   POST /api/workflow/ingest/{bookId}?host=&amp;port=         – trigger ingestion on a remote node
 *   POST /api/workflow/ingest/{bookId}/replicated
 *         ?host=&amp;port=&amp;replicaHost=&amp;replicaPort=           – ingest + replicate
 */
public class ControlController {

    private static final Logger log = LoggerFactory.getLogger(ControlController.class);

    private final HazelcastInstance hazelcast;
    private final HttpClient httpClient;

    public ControlController(HazelcastInstance hazelcast) {
        this.hazelcast  = hazelcast;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void register(Javalin app) {
        app.get("/api/health",                           this::health);
        app.get("/api/cluster/status",                   this::clusterStatus);
        app.post("/api/workflow/ingest/{bookId}",         this::ingest);
        app.post("/api/workflow/ingest/{bookId}/replicated", this::ingestAndReplicate);
    }

    // -------------------------------------------------------------------------

    private void health(Context ctx) {
        ctx.json(Map.of("status", "UP", "service", "control"));
    }

    private void clusterStatus(Context ctx) {
        int memberCount = hazelcast.getCluster().getMembers().size();
        MultiMap<String, String> idx = hazelcast.getMultiMap(Constants.INVERTED_INDEX_MAP);
        ctx.json(Map.of(
                "clusterName",  Constants.CLUSTER_NAME,
                "memberCount",  memberCount,
                "indexedTerms", idx.keySet().size()
        ));
    }

    /**
     * Delegates a download request to an Ingestion Service node.
     * Query params: host, port (of the target ingestion node)
     */
    private void ingest(Context ctx) {
        String bookId = ctx.pathParam("bookId");
        String host   = ctx.queryParam("host");
        String port   = ctx.queryParam("port");

        if (host == null || port == null) {
            ctx.status(400).json(Map.of("error", "host and port are required"));
            return;
        }

        String url = "http://" + host + ":" + port + "/api/books/" + bookId;
        try {
            String responseBody = post(url, "{}");
            ctx.json(Map.of("status", "triggered", "bookId", bookId, "response", responseBody));
        } catch (IOException e) {
            log.error("Ingest call failed", e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delegates a download-and-replicate request to an Ingestion Service node.
     * Query params: host, port (primary node), replicaHost, replicaPort (replica node)
     */
    private void ingestAndReplicate(Context ctx) {
        String bookId      = ctx.pathParam("bookId");
        String host        = ctx.queryParam("host");
        String port        = ctx.queryParam("port");
        String replicaHost = ctx.queryParam("replicaHost");
        String replicaPort = ctx.queryParam("replicaPort");

        if (host == null || port == null || replicaHost == null || replicaPort == null) {
            ctx.status(400).json(Map.of("error",
                    "host, port, replicaHost, and replicaPort are required"));
            return;
        }

        String url = "http://" + host + ":" + port
                + "/api/books/" + bookId + "/replicate"
                + "?host=" + replicaHost + "&port=" + replicaPort;
        try {
            String responseBody = post(url, "{}");
            ctx.json(Map.of("status", "triggered", "bookId", bookId, "response", responseBody));
        } catch (IOException e) {
            log.error("Ingest+replicate call failed", e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------

    private String post(String url, String jsonBody) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during POST to " + url, e);
        }
    }
}
