package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.IndexGeneration;

public final class HazelcastTokenMetrics implements TokenMetrics {

    private final HazelcastInstance hazelcast;

    public HazelcastTokenMetrics(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public void record(IndexGeneration generation, int bookId, int tokenCount) {
        tokenCounts(generation).put(bookId, tokenCount);
    }

    @Override
    public void clear(IndexGeneration generation) {
        tokenCounts(generation).clear();
    }

    private IMap<Integer, Integer> tokenCounts(IndexGeneration generation) {
        return hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.TOKEN_COUNTS,
                generation.value()
        ));
    }
}
