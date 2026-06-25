package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.model.BookIngestedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookIngestedMessageMapperTest {

    private final BookIngestedMessageMapper mapper = new BookIngestedMessageMapper(new Gson());

    @Test
    void readsTheContractPublishedByIngestion() {
        BookIngestedEvent event = mapper.fromJson("""
                {"bookId":42,"event":"document.ingested","timestamp":"2026-06-22T12:00:00Z"}
                """);

        assertEquals(42, event.bookId());
        assertEquals("document.ingested", event.event());
        assertEquals("unknown", event.sourceNodeId());
    }

    @Test
    void readsSourceNodeWhenPresent() {
        BookIngestedEvent event = mapper.fromJson("""
                {"bookId":42,"event":"document.ingested","timestamp":"2026-06-22T12:00:00Z","sourceNodeId":"10.26.14.241:5701"}
                """);

        assertEquals("10.26.14.241:5701", event.sourceNodeId());
    }

    @Test
    void rejectsUnsupportedEvents() {
        assertThrows(
                ActiveMQAdapterException.class,
                () -> mapper.fromJson("""
                        {"bookId":42,"event":"other.event","timestamp":"2026-06-22T12:00:00Z"}
                        """)
        );
    }
}
