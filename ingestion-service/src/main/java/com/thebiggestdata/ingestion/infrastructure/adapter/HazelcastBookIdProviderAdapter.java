package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.port.BookIdProviderPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class HazelcastBookIdProviderAdapter implements BookIdProviderPort {

    private final HazelcastInstance hazelcast;

    public HazelcastBookIdProviderAdapter(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public int nextBookId() {
        IMap<String, Integer> idMap = hazelcast.getMap("counters");
        String key = "book_Id";

        idMap.lock(key);
        try {
            int current = idMap.getOrDefault(key, 0);
            int nextId = current + 1;

            idMap.put(key, nextId);
            return nextId;
        } finally {
            idMap.unlock(key);
        }
    }
}