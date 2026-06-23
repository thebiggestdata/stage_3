package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexing.infrastructure.ports.ClusterTopology;

public final class HazelcastClusterTopology implements ClusterTopology {

    private static final String ROLE = "role";
    private static final String INDEXER = "indexer";
    private static final String NODE_ID = "nodeId";

    private final HazelcastInstance hazelcast;

    public HazelcastClusterTopology(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public int indexerCount() {
        return (int) hazelcast.getCluster().getMembers().stream()
                .filter(member -> INDEXER.equals(member.getAttribute(ROLE)))
                .count();
    }

    @Override
    public String localNodeId() {
        Member localMember = hazelcast.getCluster().getLocalMember();
        String configuredNodeId = localMember.getAttribute(NODE_ID);
        return configuredNodeId == null || configuredNodeId.isBlank()
                ? localMember.getUuid().toString()
                : configuredNodeId;
    }
}
