package com.thebiggestdata.ingestion.model;

import java.time.Instant;
import java.util.Objects;

public record BookIngestedEvent(
        int bookId,
        String event,
        String timestamp,
        String sourceNodeId) {

    public BookIngestedEvent {
        if (bookId < 1) {
            throw new IllegalArgumentException("bookId must be positive");
        }
        event = Objects.requireNonNull(event, "event");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        sourceNodeId = sourceNodeId == null || sourceNodeId.isBlank() ? "unknown" : sourceNodeId;
    }

    public BookIngestedEvent(int bookId, String event, String timestamp) {
        this(bookId, event, timestamp, "unknown");
    }

    public BookIngestedEvent(int bookId, String sourceNodeId) {
        this(bookId, "document.ingested", Instant.now().toString(), sourceNodeId);
    }

    public BookIngestedEvent(int bookId) {
        this(bookId, "unknown");
    }
}
