package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionCapacity;

public final class HazelcastIngestionCapacity implements IngestionCapacity {

    private static final String ROLE = "role";
    private static final String INDEXER = "indexer";

    private final HazelcastInstance hazelcast;
    private final IMap<Integer, ?> datalake;
    private final int bufferFactor;

    public HazelcastIngestionCapacity(HazelcastInstance hazelcast, int bufferFactor) {
        if (bufferFactor < 1) {
            throw new IllegalArgumentException("bufferFactor must be positive");
        }
        this.hazelcast = hazelcast;
        this.datalake = hazelcast.getMap(HazelcastNames.DATALAKE);
        this.bufferFactor = bufferFactor;
    }

    @Override
    public boolean hasRoom() {
        int indexers = indexerCount();
        return indexers > 0 && datalake.size() < bufferFactor * indexers;
    }

    private int indexerCount() {
        return (int) hazelcast.getCluster().getMembers().stream()
                .filter(this::isIndexer)
                .count();
    }

    private boolean isIndexer(Member member) {
        return INDEXER.equals(member.getAttribute(ROLE));
    }
}
