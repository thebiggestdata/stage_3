package org.example.ingestion;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.shared.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for the Ingestion Service.
 *
 * Endpoints:
 *   GET  /api/health                       – liveness probe
 *   POST /api/books/{id}                   – download book by Gutenberg ID
 *   POST /api/books/{id}/replicate?host=&port= – download + replicate to peer
 *   POST /api/replicate                    – receive a replicated file
 */
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final BookDownloader downloader;
    private final BookReplicator replicator;
    private final Gson gson = new Gson();

    public IngestionController(HazelcastInstance hazelcast) {
        this.downloader = new BookDownloader(hazelcast);
        this.replicator = new BookReplicator();
    }

    public void register(Javalin app) {
        app.get("/api/health", this::health);
        app.post("/api/books/{id}", this::downloadBook);
        app.post("/api/books/{id}/replicate", this::downloadAndReplicate);
        app.post("/api/replicate", this::receiveReplica);
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private void health(Context ctx) {
        ctx.json(Map.of("status", "UP", "service", "ingestion"));
    }

    private void downloadBook(Context ctx) {
        String bookId = ctx.pathParam("id");
        try {
            Path bodyFile = downloader.download(bookId);
            ctx.json(Map.of(
                    "status", "downloaded",
                    "bookId", bookId,
                    "path", bodyFile.toString()
            ));
        } catch (IOException e) {
            log.error("Failed to download book {}", bookId, e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private void downloadAndReplicate(Context ctx) {
        String bookId = ctx.pathParam("id");
        String host   = ctx.queryParam("host");
        String portStr = ctx.queryParam("port");

        if (host == null || portStr == null) {
            ctx.status(400).json(Map.of("error", "host and port query params are required"));
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            Path bodyFile   = downloader.download(bookId);
            Path headerFile = Paths.get(
                    bodyFile.getParent().toString(), bookId + ".header.txt");

            replicator.replicate(bodyFile, host, port);
            if (Files.exists(headerFile)) {
                replicator.replicate(headerFile, host, port);
            }

            ctx.json(Map.of(
                    "status", "downloaded-and-replicated",
                    "bookId", bookId,
                    "replicatedTo", host + ":" + port
            ));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "Invalid port: " + portStr));
        } catch (IOException e) {
            log.error("Failed to download/replicate book {}", bookId, e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    /** Receives a Base64-encoded file from a peer node and stores it locally. */
    @SuppressWarnings("unchecked")
    private void receiveReplica(Context ctx) {
        Map<String, String> body = gson.fromJson(ctx.body(), HashMap.class);
        String relativePath = body.get("relativePath");
        String content      = body.get("content");

        if (relativePath == null || content == null) {
            ctx.status(400).json(Map.of("error", "relativePath and content are required"));
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(content);
            // Store under the same datalake folder structure used by BookDownloader
            Path replicaDir = Paths.get(Constants.DATALAKE_ROOT, "replicas");
            Files.createDirectories(replicaDir);
            Path target = replicaDir.resolve(relativePath);
            Files.write(target, bytes);
            log.info("Stored replica: {}", target);
            ctx.json(Map.of("status", "stored", "path", target.toString()));
        } catch (IOException e) {
            log.error("Failed to store replica", e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
