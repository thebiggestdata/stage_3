// ingestion-service/src/main/java/com/thebiggestdata/ingestion/infrastructure/adapter/hazelcast/HazelcastDuplicationProvider.java
package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.port.DuplicationProvider;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastDuplicationProvider implements DuplicationProvider {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDuplicationProvider.class);
    private final HazelcastInstance hazelcast;
    private final NodeIdProvider nodeIdProvider;

    public HazelcastDuplicationProvider(HazelcastInstance hazelcast, NodeIdProvider nodeIdProvider) {
        this.hazelcast = hazelcast;
        this.nodeIdProvider = nodeIdProvider;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcast;
    }

    @Override
    public void duplicate(int bookId, String header, String body) {
        DuplicatedBook book = new DuplicatedBook(header, body, nodeIdProvider.nodeId());
        IMap<Integer, DuplicatedBook> datalake = hazelcast.getMap("datalake");
        datalake.put(bookId, book);
        logger.info("Book {} stored in Hazelcast RAM from node {}", bookId, nodeIdProvider.nodeId());
    }

    public int duplicateAndWait(int bookId, String header, String body, int replicationFactor) {
        duplicate(bookId, header, body);

        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        long timeout = System.currentTimeMillis() + 30000; // 30s timeout

        while (System.currentTimeMillis() < timeout) {
            Integer count = replicaCount.get(bookId);
            if (count != null && count >= replicationFactor) {
                return count;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        return replicaCount.getOrDefault(bookId, 0);
    }
}
