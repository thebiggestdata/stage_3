package com.thebiggestdata.indexer.infrastructure.adapter;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastDatalakeListener implements EntryAddedListener<Integer, DuplicatedBook> {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDatalakeListener.class);
    private final HazelcastInstance hazelcast;
    private final int replicationFactor;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.replicationFactor = replicationFactor;
    }

    @Override
    public void entryAdded(EntryEvent<Integer, DuplicatedBook> event) {
        DuplicatedBook book = event.getValue();
        int bookId = event.getKey();

        if (book == null) {
            logger.warn("Skipping null book for id {}", bookId);
            return;
        }

        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        replicaCount.lock(bookId);
        try {
            int current = replicaCount.getOrDefault(bookId, 0);
            if (current >= replicationFactor) {
                logger.debug("Book {} already indexed with {} replicas", bookId, current);
                return;
            }

            // Guardar el libro en RAM en el mapa de indexing
            IMap<Integer, DuplicatedBook> indexedBooks = hazelcast.getMap("indexed-books");
            indexedBooks.put(bookId, book);

            logger.info("Book {} indexed in RAM from node {}", bookId, book.srcNode());
        } finally {
            replicaCount.unlock(bookId);
        }
    }
}

