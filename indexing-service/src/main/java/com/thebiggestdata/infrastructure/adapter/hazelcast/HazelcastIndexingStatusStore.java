package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.thebiggestdata.domain.gateway.IndexingStatusRepository;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastIndexingStatusStore implements IndexingStatusRepository {

    private final ISet<Integer> registry;

    public HazelcastIndexingStatusStore(HazelcastInstance hz) {
        this.registry = hz.getSet("indexingRegistry");
    }

    @Override
    public boolean markAsIndexed(int documentId) {
        return registry.add(documentId);
    }
}
