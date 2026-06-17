package com.thebiggestdata.ingestion.model;

import java.time.Instant;

public record DocumentIngestedEvent(int bookId) {
    private final static String EVENT = "document.ingested";
    private final static String TS = Instant.now().toString();
}
