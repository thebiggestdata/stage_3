package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildState;

import java.util.concurrent.TimeUnit;

public final class HazelcastRebuildState implements RebuildState {

    static final String ACTIVE_KEY = "active";

    private final IMap<String, String> state;

    public HazelcastRebuildState(HazelcastInstance hazelcast) {
        this.state = hazelcast.getMap(HazelcastNames.REBUILD_STATE);
    }

    private boolean isActive() {
        return state.containsKey(ACTIVE_KEY);
    }

    @Override
    public void awaitCompletion() {
        while (isActive()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HazelcastAdapterException("Interrupted while waiting for rebuild", e);
            }
        }
    }
}
