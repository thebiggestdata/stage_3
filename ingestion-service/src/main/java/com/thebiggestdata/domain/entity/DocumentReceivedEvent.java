package com.thebiggestdata.domain.entity;

import java.time.Instant;

public record DocumentReceivedEvent(int bookId) {
    private final static String EVENT = "document.ingested";
    private final static String TS = Instant.now().toString();
}
