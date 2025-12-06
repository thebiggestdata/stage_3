package com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DuplicationProvider;

public class HazelcastDuplicationProvider implements DuplicationProvider {
    private final HazelcastInstance hazelcast;
    private final NodeIdProvider nodeInfoProvider;

    public HazelcastDuplicationProvider(HazelcastInstance hazelcast, NodeIdProvider nodeInfoProvider) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
    }

    @Override
    public void duplicate(int bookId, String header, String body) {
        IMap<Integer, Boolean> lockMap = hazelcast.getMap("book-locks");
        lockMap.lock(bookId);
        try {
            MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");
            datalake.put(bookId, new DuplicatedBook(header, body, nodeInfoProvider.nodeId()));
            System.out.println("Current MultiMap keys: " + datalake.keySet());
        } finally {
            lockMap.unlock(bookId);
        }
    }
}
