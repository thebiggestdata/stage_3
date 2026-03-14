package org.example.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Replicates a local file to a remote Ingestion Service node so that every
 * stored book is present on at least {@code REPLICATION_FACTOR} nodes.
 *
 * The remote node must expose {@code POST /api/replicate} that accepts a JSON
 * body containing the relative path and the Base64-encoded file contents.
 */
public class BookReplicator {

    private static final Logger log = LoggerFactory.getLogger(BookReplicator.class);

    private final HttpClient httpClient;

    public BookReplicator() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends {@code filePath} to the given remote host:port.
     *
     * @param filePath     absolute or relative path of the file to replicate
     * @param remoteHost   IP / hostname of the target node
     * @param remotePort   port of the remote Ingestion Service
     */
    public void replicate(Path filePath, String remoteHost, int remotePort) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        String encoded = Base64.getEncoder().encodeToString(bytes);

        // Build a minimal JSON payload
        String json = String.format(
                "{\"relativePath\":\"%s\",\"content\":\"%s\"}",
                filePath.getFileName().toString(),
                encoded
        );

        String url = "http://" + remoteHost + ":" + remotePort + "/api/replicate";
        log.info("Replicating {} to {}", filePath.getFileName(), url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Replication to {} returned HTTP {}", url, response.statusCode());
            } else {
                log.info("Replication to {} succeeded", url);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during replication to " + url, e);
        }
    }
}
