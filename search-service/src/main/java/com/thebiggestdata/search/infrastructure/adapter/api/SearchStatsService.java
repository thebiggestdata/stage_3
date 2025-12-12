package com.thebiggestdata.search.infrastructure.adapter.api;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.infrastructure.port.SearchStatsProvider;
import java.util.Map;

public class SearchStatsService implements SearchStatsProvider {
    private final HazelcastInstance hazelcast;

    public SearchStatsService(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<String, Object> stats() {
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