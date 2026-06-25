package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.infrastructure.ports.HealthProbe;
import com.thebiggestdata.search.model.IndexGeneration;

public final class HazelcastHealthProbe implements HealthProbe {

    private final HazelcastInstance hazelcast;

    public HazelcastHealthProbe(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public int indexedDocumentCount(IndexGeneration generation) {
        return hazelcast.getSet(HazelcastNames.generated(
                HazelcastNames.INDEXED_BOOKS,
                generation.value()
        )).size();
    }

    @Override
    public String localNodeId() {
        Member member = hazelcast.getCluster().getLocalMember();
        String nodeId = member.getAttribute("nodeId");
        return nodeId == null || nodeId.isBlank() ? member.getUuid().toString() : nodeId;
    }
}
