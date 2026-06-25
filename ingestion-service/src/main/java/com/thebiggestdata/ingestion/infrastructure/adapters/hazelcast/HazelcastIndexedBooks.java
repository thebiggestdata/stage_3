package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.IndexedBooks;

public final class HazelcastIndexedBooks implements IndexedBooks {

    private static final String ACTIVE_GENERATION = "active";
    private static final String INITIAL_GENERATION = "initial";

    private final HazelcastInstance hazelcast;
    private final IMap<String, String> generations;

    public HazelcastIndexedBooks(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        this.generations = hazelcast.getMap(HazelcastNames.INDEX_GENERATIONS);
    }

    @Override
    public boolean has(int bookId) {
        String generation = generations.getOrDefault(ACTIVE_GENERATION, INITIAL_GENERATION);
        return hazelcast.<Integer>getSet(generatedName(generation)).contains(bookId);
    }

    private String generatedName(String generation) {
        return HazelcastNames.INDEXED_BOOKS + ":" + generation;
    }
}
