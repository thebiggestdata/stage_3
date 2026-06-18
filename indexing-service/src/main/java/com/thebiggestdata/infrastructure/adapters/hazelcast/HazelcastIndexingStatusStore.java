package com.thebiggestdata.infrastructure.adapters.hazelcast;

import com.thebiggestdata.infrastructure.ports.IndexingStatusStore;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastIndexingStatusStore implements IndexingStatusStore {

    private final ISet<Integer> registry;

    public HazelcastIndexingStatusStore(HazelcastInstance hz) {
        this.registry = hz.getSet("indexingRegistry");
    }

    @Override
    public boolean markAsIndexed(int documentId) {
        return registry.add(documentId);
    }
}
