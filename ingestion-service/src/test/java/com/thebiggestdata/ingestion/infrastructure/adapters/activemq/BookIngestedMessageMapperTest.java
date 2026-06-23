package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookIngestedMessageMapperTest {

    @Test
    void createsTheStableIndexingEventContract() {
        BookIngestedMessageMapper mapper = new BookIngestedMessageMapper(new Gson());
        BookIngestedEvent event = new BookIngestedEvent(
                42,
                "document.ingested",
                "2026-06-22T12:00:00Z"
        );

        JsonObject json = JsonParser.parseString(mapper.toJson(event)).getAsJsonObject();
        Map<String, Object> properties = mapper.toProperties(event);

        assertEquals(42, json.get("bookId").getAsInt());
        assertEquals("document.ingested", json.get("event").getAsString());
        assertEquals("2026-06-22T12:00:00Z", json.get("timestamp").getAsString());
        assertEquals(42, properties.get("bookId"));
        assertEquals("document.ingested", properties.get("event"));
    }
}
