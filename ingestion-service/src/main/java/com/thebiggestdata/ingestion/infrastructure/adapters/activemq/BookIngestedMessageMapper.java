package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;

import java.util.Map;

public final class BookIngestedMessageMapper {

    private final Gson gson;

    public BookIngestedMessageMapper(Gson gson) {
        this.gson = gson;
    }

    public String toJson(BookIngestedEvent event) {
        return gson.toJson(event);
    }

    public Map<String, Object> toProperties(BookIngestedEvent event) {
        return Map.of(
                "bookId", event.bookId(),
                "event", event.event(),
                "timestamp", event.timestamp(),
                "sourceNodeId", event.sourceNodeId()
        );
    }
}
