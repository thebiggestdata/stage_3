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
                logger.debug("Datalake now contains {} books", datalake.keySet().size());
            } else {
                logger.warn("Book {} was already in the datalake", bookId);
            }

        } catch (Exception e) {
            logger.error("Error duplicating book {} to cluster: {}", bookId, e.getMessage(), e);
            throw new RuntimeException("Failed to duplicate book to cluster", e);
        } finally {
            lockMap.unlock(bookId);
        }
    }

    public boolean exists(int bookId) {
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap(DATALAKE_MAP);
        return datalake.containsKey(bookId);
    }

    public int getReplicaCount(int bookId) {
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap(DATALAKE_MAP);
        return datalake.get(bookId).size();
    }
}