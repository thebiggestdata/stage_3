package com.thebiggestdata.indexing.model;

import java.time.Instant;

public record IngestionControlEvent(Type action, String event, String timestamp) {

    public enum Type {
        PAUSED,
        RESUMED
    }

    public IngestionControlEvent(Type type) {
        this(type, "ingestion.control", Instant.now().toString());
    }
}
