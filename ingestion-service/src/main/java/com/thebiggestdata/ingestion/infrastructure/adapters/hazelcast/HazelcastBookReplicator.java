package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookReplicator;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.ReplicationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class HazelcastBookReplicator implements BookReplicator {

    private static final Logger log = LoggerFactory.getLogger(HazelcastBookReplicator.class);

    private final HazelcastInstance hazelcast;
    private final String localNodeId;
    private final int replicationFactor;
    private final Duration timeout;
    private final Duration checkInterval;
    private final HazelcastReplicaRegistry replicaRegistry;

    public HazelcastBookReplicator(
            HazelcastInstance hazelcast,
            String localNodeId,
            int replicationFactor,
            Duration timeout,
            Duration checkInterval
    ) {
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("replicationFactor must be positive");
        }
        this.hazelcast = hazelcast;
        this.localNodeId = localNodeId;
        this.replicationFactor = replicationFactor;
        this.timeout = timeout;
        this.checkInterval = checkInterval;
        this.replicaRegistry = new HazelcastReplicaRegistry(hazelcast);
    }

    @Override
    public ReplicationResult replicate(Book book) {
        replicaRegistry.register(book.bookId(), localNodeId);
        int effectiveReplicationFactor = effectiveReplicationFactor();
        requestReplicas(book.bookId(), effectiveReplicationFactor);
        ReplicationResult result = awaitReplication(book.bookId(), effectiveReplicationFactor);

        if (!result.isSatisfied()) {
            throw new HazelcastAdapterException(
                    "Replication factor %d not reached for book %d; replicas=%s"
                            .formatted(effectiveReplicationFactor, book.bookId(), result.replicaNodeIds())
            );
        }
        return result;
    }

    private int effectiveReplicationFactor() {
        return Math.max(1, Math.min(replicationFactor, replicaRegistry.activeIngestionNodeIds().size()));
    }

    private void requestReplicas(int bookId, int effectiveReplicationFactor) {
        List<String> targets = replicaTargets(bookId, effectiveReplicationFactor);
        try {
            for (String target : targets) {
                IQueue<Integer> queue = hazelcast.getQueue(HazelcastNames.replicationQueueFor(target));
                queue.put(bookId);
                log.info("REPLICA_REQUESTED bookId={} source={} target={}", bookId, localNodeId, target);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HazelcastAdapterException("Interrupted while requesting replicas", e);
        }
    }

    private List<String> replicaTargets(int bookId, int effectiveReplicationFactor) {
        int replicasNeeded = effectiveReplicationFactor - 1;
        if (replicasNeeded == 0) {
            return List.of();
        }

        List<String> peers = replicaRegistry.activeIngestionNodeIds().stream()
                .filter(nodeId -> !nodeId.equals(localNodeId))
                .toList();
        if (peers.isEmpty()) {
            return List.of();
        }

        int first = Math.floorMod(bookId, peers.size());
        return java.util.stream.IntStream.range(0, Math.min(replicasNeeded, peers.size()))
                .mapToObj(offset -> peers.get((first + offset) % peers.size()))
                .toList();
    }

    private ReplicationResult awaitReplication(int bookId, int effectiveReplicationFactor) {
        Instant deadline = Instant.now().plus(timeout);
        List<String> nodes = replicaRegistry.nodesFor(bookId);

        while (nodes.size() < effectiveReplicationFactor && Instant.now().isBefore(deadline)) {
            sleep();
            nodes = replicaRegistry.nodesFor(bookId);
        }
        return new ReplicationResult(bookId, effectiveReplicationFactor, nodes);
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(checkInterval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HazelcastAdapterException("Interrupted while waiting for replication", e);
        }
    }
}
