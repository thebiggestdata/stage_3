package com.thebiggestdata.ingestion.model;

import java.time.Instant;

public record IngestionStateEvent(Type action, String event, String timestamp) {
    public enum Type {
        PAUSED,
        RESUMED
    }

    public IngestionStateEvent(Type type) {
        this(type, "ingestion.control",Instant.now().toString()); // TODO maybe erase this?
    }
}
