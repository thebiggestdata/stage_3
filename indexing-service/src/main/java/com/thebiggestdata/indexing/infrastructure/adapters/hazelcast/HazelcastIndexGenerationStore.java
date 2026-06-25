package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.model.IndexGeneration;

public final class HazelcastIndexGenerationStore implements IndexGenerationStore {

    private static final String ACTIVE = "active";
    private static final String INITIAL = "initial";

    private final IMap<String, String> generations;

    public HazelcastIndexGenerationStore(HazelcastInstance hazelcast) {
        this.generations = hazelcast.getMap(HazelcastNames.INDEX_GENERATIONS);
    }

    @Override
    public IndexGeneration active() {
        String generation = generations.putIfAbsent(ACTIVE, INITIAL);
        return new IndexGeneration(generation == null ? INITIAL : generation);
    }

    @Override
    public void prepare(IndexGeneration generation) {
        generations.put("prepared:" + generation.value(), generation.value());
    }

    @Override
    public void activate(IndexGeneration generation) {
        generations.set(ACTIVE, generation.value());
        generations.remove("prepared:" + generation.value());
    }
}
