package com.thebiggestdata.infrastructure.adapter.cluster;

import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.domain.gateway.IngestionQueueStore;

import java.util.concurrent.TimeUnit;

public class HazelcastIngestionQueueStore implements IngestionQueueStore {

    private final HazelcastInstance hz;

    public HazelcastIngestionQueueStore(HazelcastInstance hz) {
        this.hz = hz;
    }

    @Override
    public Integer pollNextBook() {
        IQueue<Integer> queue = hz.getQueue("books");
        try {
            return queue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean isBookIndexed(int bookId) {
        ISet<Integer> registry = hz.getSet("indexingRegistry");
        return registry.contains(bookId);
    }

    @Override
    public int getDatalakeSize() {
        IMap<Object, Object> datalake = hz.getMap("datalake");
        return datalake.size();
    }

    @Override
    public int getIndexerNodeCount() {
        return (int) hz.getCluster().getMembers().stream()
                .filter(m -> "indexer".equals(m.getAttribute("role")))
                .count();
    }
}