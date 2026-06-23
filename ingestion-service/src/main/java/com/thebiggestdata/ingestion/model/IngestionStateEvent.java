package com.thebiggestdata.ingestion.model;

import java.time.Instant;
import java.util.Objects;

public record IngestionStateEvent(Type action, String event, String timestamp) {

    public IngestionStateEvent {
        action = Objects.requireNonNull(action, "action");
        event = Objects.requireNonNull(event, "event");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public enum Type {
        PAUSED,
        RESUMED
    }

    public IngestionStateEvent(Type type) {
        this(type, "ingestion.control", Instant.now().toString());
    }
}
