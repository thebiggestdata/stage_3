package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thebiggestdata.ingestion.model.IngestionStateEvent;

import java.time.Instant;

public final class IngestionControlMessageMapper {

    private static final String EVENT_NAME = "ingestion.control";

    private final Gson gson;

    public IngestionControlMessageMapper(Gson gson) {
        this.gson = gson;
    }

    public IngestionStateEvent toEvent(String json) {
        try {
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            if (payload.has("action")) {
                return gson.fromJson(payload, IngestionStateEvent.class);
            }
            return legacyEvent(payload);
        } catch (RuntimeException e) {
            throw new ActiveMQAdapterException("Invalid ingestion control event", e);
        }
    }

    private IngestionStateEvent legacyEvent(JsonObject payload) {
        String legacyType = requiredString(payload, "type");
        IngestionStateEvent.Type action = switch (legacyType) {
            case "INGESTION_PAUSE" -> IngestionStateEvent.Type.PAUSED;
            case "INGESTION_RESUME" -> IngestionStateEvent.Type.RESUMED;
            default -> throw new IllegalArgumentException("Unknown ingestion control type: " + legacyType);
        };
        String timestamp = payload.has("ts")
                ? payload.get("ts").getAsString()
                : Instant.now().toString();
        return new IngestionStateEvent(action, EVENT_NAME, timestamp);
    }

    private String requiredString(JsonObject payload, String field) {
        if (!payload.has(field) || payload.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing event field: " + field);
        }
        return payload.get(field).getAsString();
    }
}
