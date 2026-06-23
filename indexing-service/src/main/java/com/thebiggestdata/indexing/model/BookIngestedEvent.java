package com.thebiggestdata.indexing.model;

public record BookIngestedEvent(int bookId, String event, String timestamp) {

    public BookIngestedEvent {
        if (bookId < 1) {
            throw new IllegalArgumentException("bookId must be positive");
        }
        if (!"document.ingested".equals(event)) {
            throw new IllegalArgumentException("Unsupported event: " + event);
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("timestamp must not be blank");
        }
    }
}
