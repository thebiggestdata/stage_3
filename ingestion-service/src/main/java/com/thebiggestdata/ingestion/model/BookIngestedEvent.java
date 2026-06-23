package com.thebiggestdata.ingestion.model;

import java.time.Instant;
import java.util.Objects;

public record BookIngestedEvent(
        int bookId,
        String event,
        String timestamp) {

    public BookIngestedEvent {
        if (bookId < 1) {
            throw new IllegalArgumentException("bookId must be positive");
        }
        event = Objects.requireNonNull(event, "event");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public BookIngestedEvent(int bookId) {
        this(bookId, "document.ingested", Instant.now().toString());
    }
}
