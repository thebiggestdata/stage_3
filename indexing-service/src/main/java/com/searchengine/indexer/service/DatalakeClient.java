package com.searchengine.indexer.service;

import com.searchengine.indexer.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DatalakeClient {

    private static final Logger logger = LoggerFactory.getLogger(DatalakeClient.class);

    private final Path datalakeRoot;

    public DatalakeClient(String datalakePath) {
        this.datalakeRoot = Path.of(datalakePath);
        logger.info("DatalakeClient initialized with root: {}", datalakePath);
    }

    public Document readDocument(int bookId) {
        String bodyPath = resolveBodyPath(bookId);
        String content = readContent(bodyPath);
        return new Document(bookId, content);
    }

    private String resolveBodyPath(int bookId) {
        String pattern = bookId + "_body.txt";

        try (Stream<Path> stream = Files.walk(datalakeRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(pattern))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Body not found for book " + bookId))
                    .toString();
        } catch (IOException e) {
            logger.error("Failed to search datalake for book {}: {}", bookId, e.getMessage());
            throw new RuntimeException("Failed searching datalake", e);
        }
    }

    private String readContent(String path) {
        try {
            String content = Files.readString(Path.of(path));
            logger.debug("Read {} characters from {}", content.length(), path);
            return content;
        } catch (IOException e) {
            logger.error("Failed to read file {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to read " + path, e);
        }
    }
}

