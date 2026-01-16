package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastDatalakeListener extends HzlcstEntryListener<Integer, DuplicatedBook> {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDatalakeListener.class);
    private final NodeIdProvider nodeIdProvider;
    private final int replicationFactor;
    private final HazelcastInstance hazelcast;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast, NodeIdProvider nodeIdProvider, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeIdProvider = nodeIdProvider;
        this.replicationFactor = replicationFactor;
    }

    @Override
    public void entryAdded(EntryEvent<Integer, DuplicatedBook> event) {
        DuplicatedBook replicated = event.getValue();
        int bookId = event.getKey();

        if (replicated == null || replicated.srcNode() == null) {
            logger.warn("Skipping event for book {}: srcNode is null", bookId);
            return;
        }

        if (replicated.srcNode().equals(nodeIdProvider.nodeId())) return;

        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        replicaCount.lock(bookId);
        try {
            int current = replicaCount.getOrDefault(bookId, 0);
            if (current >= replicationFactor) {
                logger.debug("Book {} already has {} replicas, skipping", bookId, current);
                return;
            }
            replicaCount.put(bookId, current + 1);
            logger.info("Book {} replica count: {}/{} (in RAM only)", bookId, current + 1, replicationFactor);
        } finally {
            replicaCount.unlock(bookId);
        }

        // Ya no escribir en disco, solo actualizar contador
        logger.info("Book {} replicated in RAM from node {}", bookId, replicated.srcNode());
    }
}
