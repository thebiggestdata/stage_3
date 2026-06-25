package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.search.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.search.model.IndexGeneration;

public final class HazelcastIndexGenerationStore implements IndexGenerationStore {

    private static final String ACTIVE = "active";
    private static final String INITIAL = "initial";

    private final IMap<String, String> generations;

    public HazelcastIndexGenerationStore(HazelcastInstance hazelcast) {
        this.generations = hazelcast.getMap(HazelcastNames.INDEX_GENERATIONS);
    }

    @Override
    public IndexGeneration active() {
        return new IndexGeneration(generations.getOrDefault(ACTIVE, INITIAL));
    }
}
