package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.ReplicationExecuter;
import com.thebiggestdata.ingestion.model.Node;

import java.util.HashSet;
import java.util.Set;

public class HazelcastReplicationExecuter implements ReplicationExecuter {

    private final HazelcastInstance hazelcast;
    private final Node node;
    private final int replicationFactor;

    public HazelcastReplicationExecuter(HazelcastInstance hazelcast, Node node,
                                        int replicationFactor) {
        this.hazelcast = hazelcast;
        this.node = node;
        this.replicationFactor = replicationFactor;
    }

    public void execute(int bookId) {
        addLocalNodeToReplicatedMap(bookId);
        replicate(bookId);
    }

    private void addLocalNodeToReplicatedMap(int bookId) {
        IMap<Integer, Set<String>> replicatedNodesMap = hazelcast.getMap("replicatedNodesMap");

        replicatedNodesMap.lock(bookId);
        try {
            Set<String> nodes = replicatedNodesMap.getOrDefault(bookId, new HashSet<>());
            nodes.add(node.nodeId());
            replicatedNodesMap.put(bookId, nodes);
        } finally {
            replicatedNodesMap.unlock(bookId);
        }
    }

    @Override
    public void replicate(int bookId) {
        IQueue<Integer> booksToBeReplicated = hazelcast.getQueue("booksToBeReplicated");
        try {
            for (int i = 1; i < replicationFactor; i++) {
                booksToBeReplicated.put(bookId);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
