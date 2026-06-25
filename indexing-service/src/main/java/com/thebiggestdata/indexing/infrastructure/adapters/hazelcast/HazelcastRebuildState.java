package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class HazelcastRebuildState implements RebuildState {

    private static final Logger log = LoggerFactory.getLogger(HazelcastRebuildState.class);

    static final String ACTIVE_KEY = "active";

    private final IMap<String, String> state;

    public HazelcastRebuildState(HazelcastInstance hazelcast) {
        this.state = hazelcast.getMap(HazelcastNames.REBUILD_STATE);
    }

    @Override
    public void awaitCompletion() {
        String rebuildId = state.get(ACTIVE_KEY);
        boolean waitingWasLogged = false;
        while (rebuildId != null) {
            if (!waitingWasLogged) {
                log.info("INDEXING_WAITING reason=rebuild rebuildId={}", rebuildId);
                waitingWasLogged = true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HazelcastAdapterException("Interrupted while waiting for rebuild", e);
            }
            rebuildId = state.get(ACTIVE_KEY);
        }
    }
}
