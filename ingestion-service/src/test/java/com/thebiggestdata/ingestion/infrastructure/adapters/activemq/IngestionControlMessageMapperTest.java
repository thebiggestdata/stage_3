package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.model.IngestionStateEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionControlMessageMapperTest {

    private final IngestionControlMessageMapper mapper = new IngestionControlMessageMapper(new Gson());

    @Test
    void readsTheCanonicalControlEvent() {
        IngestionStateEvent event = mapper.toEvent("""
                {"action":"PAUSED","event":"ingestion.control","timestamp":"2026-06-22T12:00:00Z"}
                """);

        assertEquals(IngestionStateEvent.Type.PAUSED, event.action());
        assertEquals("2026-06-22T12:00:00Z", event.timestamp());
    }

    @Test
    void temporarilyAcceptsTheLegacyIndexerEvent() {
        IngestionStateEvent event = mapper.toEvent("""
                {"type":"INGESTION_RESUME","event":"ingestion.control","ts":"2026-06-22T12:00:00Z"}
                """);

        assertEquals(IngestionStateEvent.Type.RESUMED, event.action());
        assertEquals("2026-06-22T12:00:00Z", event.timestamp());
    }

    @Test
    void rejectsMalformedEvents() {
        assertThrows(ActiveMQAdapterException.class, () -> mapper.toEvent("{}"));
    }
}
