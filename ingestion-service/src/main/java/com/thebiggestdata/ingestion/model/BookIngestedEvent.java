package com.thebiggestdata.ingestion.model;

import java.time.Instant;

public record BookIngestedEvent(int bookId, String event, String timestamp) {
    public BookIngestedEvent(int bookId){
        this(bookId, "document.ingested", Instant.now().toString());
    }
}
