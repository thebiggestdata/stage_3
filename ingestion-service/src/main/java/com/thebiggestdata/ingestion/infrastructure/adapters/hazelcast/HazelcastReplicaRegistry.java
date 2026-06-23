package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.cluster.Member;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class HazelcastReplicaRegistry {

    private static final String NODE_ID = "nodeId";

    private final HazelcastInstance hazelcast;
    private final IMap<Integer, Set<String>> replicas;

    HazelcastReplicaRegistry(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
        this.replicas = hazelcast.getMap(HazelcastNames.REPLICATED_NODES);
    }

    void register(int bookId, String nodeId) {
        replicas.lock(bookId);
        try {
            Set<String> nodes = new HashSet<>(replicas.getOrDefault(bookId, Set.of()));
            nodes.add(nodeId);
            replicas.put(bookId, nodes);
        } finally {
            replicas.unlock(bookId);
        }
    }

    boolean contains(int bookId, String nodeId) {
        Set<String> nodes = replicas.get(bookId);
        return nodes != null && nodes.contains(nodeId);
    }

    List<String> nodesFor(int bookId) {
        Set<String> activeNodeIds = hazelcast.getCluster().getMembers().stream()
                .flatMap(member -> nodeIds(member).stream())
                .collect(java.util.stream.Collectors.toSet());
        return replicas.getOrDefault(bookId, Set.of()).stream()
                .filter(activeNodeIds::contains)
                .sorted()
                .toList();
    }

    private Set<String> nodeIds(Member member) {
        String configuredNodeId = member.getAttribute(NODE_ID);
        if (configuredNodeId == null || configuredNodeId.isBlank()) {
            return Set.of(member.getUuid().toString());
        }
        return Set.of(configuredNodeId, member.getUuid().toString());
    }
}
