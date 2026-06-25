package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.model.BookIngestedEvent;

public final class BookIngestedMessageMapper {

    private final Gson gson;

    public BookIngestedMessageMapper(Gson gson) {
        this.gson = gson;
    }

    public BookIngestedEvent fromJson(String json) {
        try {
            return gson.fromJson(json, BookIngestedEvent.class);
        } catch (RuntimeException e) {
            throw new ActiveMQAdapterException("Invalid document.ingested event", e);
        }
    }
}
