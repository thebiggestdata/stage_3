package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastDuplicationProvider {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDuplicationProvider.class);
    private static final String DATALAKE_MAP = "datalake";
    private static final String LOCK_MAP = "book-locks";
    private final HazelcastInstance hazelcast;
    private final NodeIdProvider nodeIdProvider;

    public HazelcastDuplicationProvider(HazelcastInstance hazelcast, NodeIdProvider nodeIdProvider) {
        this.hazelcast = hazelcast;
        this.nodeIdProvider = nodeIdProvider;
    }

    public void duplicate(int bookId, String header, String body) {
        IMap<Integer, Boolean> lockMap = hazelcast.getMap(LOCK_MAP);
        lockMap.lock(bookId);
        try {
            MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap(DATALAKE_MAP);
            DuplicatedBook book = new DuplicatedBook(header, body, nodeIdProvider.nodeId());
            boolean added = datalake.put(bookId, book);
            if (added) {
                logger.info("Book {} replicated to cluster from node {}", bookId, nodeIdProvider.nodeId());
            }
        } finally {
            lockMap.unlock(bookId);
        }
    }

    public int duplicateAndWait(int bookId, String header, String body, int requiredReplicas) {
        duplicate(bookId, header, body);
        int retries = 0;
        while (retries < 20) {
            int count = getReplicaCount(bookId);
            if (count >= requiredReplicas) {
                logger.info("Book {} has {} replicas (required: {})", bookId, count, requiredReplicas);
                return count;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            retries++;
        }
        throw new RuntimeException("Replication failed for book " + bookId);
    }

    public int getReplicaCount(int bookId) {
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap(DATALAKE_MAP);
        int clusterSize = hazelcast.getCluster().getMembers().size();
        if (clusterSize == 1) return datalake.get(bookId).size();
        String currentNode = nodeIdProvider.nodeId();
        return (int) datalake.get(bookId).stream()
                .filter(book -> !book.srcNode().equals(currentNode))
                .count();
    }

    public HazelcastInstance getHazelcastInstance() {return this.hazelcast;}
}