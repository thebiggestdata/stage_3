package com.thebiggestdata.domain.entity;

import java.time.Instant;

public record IngestionControlEvent(Type type, String ts, String event) {
    public enum Type {
        INGESTION_PAUSE,
        INGESTION_RESUME
    }

    public IngestionControlEvent(Type type) {
        this(type, Instant.now().toString(), "ingestion.control");
    }
}
