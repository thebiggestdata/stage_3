package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
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

    @Override
    public void duplicate(int bookId, String header, String body) {
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");
        DuplicatedBook book = new DuplicatedBook(header, body, nodeIdProvider.nodeId());
        datalake.put(bookId, book);
        logger.info("Book {} stored in RAM datalake from node {}", bookId, nodeIdProvider.nodeId());
    }
}
