package com.thebiggestdata.indexer.infrastructure.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.indexer.domain.exception.DocumentReadException;
import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.port.DatalakeReadPort;
import com.thebiggestdata.ingestion.model.DuplicatedBook;

import java.util.Collection;

public class HazelcastDatalakeAdapter implements DatalakeReadPort {
    private final HazelcastInstance hazelcast;

    public HazelcastDatalakeAdapter(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public RawDocument read(int bookId, String ignoredPath) {
        // En RAM usamos MultiMap o IMap segun corresponda, asumiendo MultiMap por la estructura del projecto
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");

        Collection<DuplicatedBook> values = datalake.get(bookId);
        if (values == null || values.isEmpty()) {
            throw new DocumentReadException("Book " + bookId + " not found in RAM datalake", null);
        }

        // Tomamos cualquier réplica disponible
        DuplicatedBook book = values.iterator().next();
        return new RawDocument(bookId, book.body());
    }
}
