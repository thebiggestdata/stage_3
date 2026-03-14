package org.example.ingestion;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import org.example.shared.Constants;
import org.example.shared.HazelcastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Downloads a book from Project Gutenberg, splits it into header/body files,
 * saves them to the local datalake, and publishes a Hazelcast topic event so
 * that the Indexing Service can pick it up.
 */
public class BookDownloader {

    private static final Logger log = LoggerFactory.getLogger(BookDownloader.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH");

    private final HazelcastInstance hazelcast;

    public BookDownloader(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    /**
     * Downloads, parses, and stores the book. Returns the path to the body file.
     */
    public Path download(String bookId) throws IOException {
        log.info("Downloading book ID: {}", bookId);

        String url = String.format(Constants.GUTENBERG_BASE_URL, bookId, bookId);
        String rawText = fetchUrl(url);

        String[] parts = splitText(rawText);
        String header = parts[0];
        String body   = parts[1];

        Path dir = buildDatalakePath();
        Files.createDirectories(dir);

        Path headerFile = dir.resolve(bookId + ".header.txt");
        Path bodyFile   = dir.resolve(bookId + ".body.txt");

        Files.writeString(headerFile, header);
        Files.writeString(bodyFile, body);

        log.info("Saved book {} -> {}", bookId, dir);

        // Notify indexing service via Hazelcast topic
        ITopic<String> topic = hazelcast.getTopic(Constants.BOOK_DOWNLOADED_TOPIC);
        topic.publish(bodyFile.toAbsolutePath().toString());

        return bodyFile;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String fetchUrl(String url) throws IOException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .GET()
                .build();
        try {
            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
    }

    /** Splits raw Gutenberg text into [header, body]. */
    static String[] splitText(String raw) {
        int startIdx = findMarker(raw, Constants.BODY_START_MARKER);
        int endIdx   = findMarker(raw, Constants.BODY_END_MARKER);

        String header;
        String body;

        if (startIdx >= 0) {
            // Everything before (and including) the start-of-body line is the header
            int lineEnd = raw.indexOf('\n', startIdx);
            header = (lineEnd >= 0) ? raw.substring(0, lineEnd).trim() : raw.substring(0, startIdx).trim();
            int bodyStart = (lineEnd >= 0) ? lineEnd + 1 : startIdx;
            if (endIdx > bodyStart) {
                body = raw.substring(bodyStart, endIdx).trim();
            } else {
                body = raw.substring(bodyStart).trim();
            }
        } else {
            // No Gutenberg markers — treat whole text as body
            header = "";
            body   = raw.trim();
        }

        return new String[]{header, body};
    }

    private static int findMarker(String text, String marker) {
        // Case-insensitive search
        return text.toUpperCase().indexOf(marker.toUpperCase());
    }

    private Path buildDatalakePath() {
        LocalDateTime now = LocalDateTime.now();
        return Paths.get(Constants.DATALAKE_ROOT,
                now.format(DATE_FMT),
                now.format(HOUR_FMT));
    }
}
