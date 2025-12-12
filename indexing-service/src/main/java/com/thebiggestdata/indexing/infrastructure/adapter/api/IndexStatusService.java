package com.thebiggestdata.indexing.infrastructure.adapter.api;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexing.infrastructure.port.IndexStatusProvider;

import java.util.Map;

public class IndexStatusService implements IndexStatusProvider {
    private final HazelcastInstance hazelcast;

    public IndexStatusService(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<String, Object> status() {
        int indexedBooks = hazelcast.getMap("indexed-books").size();
        int totalTerms = hazelcast.getMultiMap("inverted-index").keySet().size();
        int clusterSize = hazelcast.getCluster().getMembers().size();

        return Map.of(
                "status", "running",
                "indexed_books", indexedBooks,
                "total_terms", totalTerms,
                "cluster_members", clusterSize
        );
    }
}