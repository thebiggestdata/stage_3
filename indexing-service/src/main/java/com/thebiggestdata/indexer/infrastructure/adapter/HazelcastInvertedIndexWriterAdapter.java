package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.InvertedIndexWriterPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HazelcastInvertedIndexWriterAdapter implements InvertedIndexWriterPort {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastInvertedIndexWriterAdapter.class);
    private final HazelcastInstance hazelcast;
    private final String mapName;

    public HazelcastInvertedIndexWriterAdapter(HazelcastInstance hazelcast, String mapName) {
        this.hazelcast = hazelcast;
        this.mapName = mapName;
    }

    @Override
    public void write(Map<String, List<Integer>> postings) {
        MultiMap<String, String> mm = hazelcast.getMultiMap(mapName);

        postings.forEach((token, ids) -> {
            try {
                if (mm.tryLock(token, 10, TimeUnit.SECONDS)) {
                    try {
                        for (Integer id : ids) {
                            String idStr = String.valueOf(id);
                            if (!mm.get(token).contains(idStr)) {
                                mm.put(token, idStr);
                                logger.debug("Indexed docId {} for token '{}'", id, token);
                            }
                        }
                    } finally {
                        mm.unlock(token);
                    }
                } else {
                    logger.warn("Could not acquire lock for token '{}' within 10 seconds", token);
                    throw new RuntimeException("Lock acquisition timeout for token: " + token);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted while waiting for lock on token '{}'", token);
                throw new RuntimeException("Interrupted during lock acquisition", e);
            }
        });
    }
}
