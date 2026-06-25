package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GutenbergRetryPolicyTest {

    @Test
    void doesNotRetryMissingBooks() {
        AtomicInteger requests = new AtomicInteger();
        GutenbergRetryPolicy retryPolicy = new GutenbergRetryPolicy(3, 1);

        assertThrows(GutenbergAdapterException.class, () -> retryPolicy.execute(() -> {
            requests.incrementAndGet();
            throw new GutenbergHttpException(404, "Not found");
        }));

        assertEquals(1, requests.get());
    }
}
