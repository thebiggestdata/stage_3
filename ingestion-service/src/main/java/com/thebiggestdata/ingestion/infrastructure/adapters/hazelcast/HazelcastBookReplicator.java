package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookReplicator;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.ReplicationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class HazelcastBookReplicator implements BookReplicator {

    private final String localNodeId;
    private final int replicationFactor;
    private final Duration timeout;
    private final Duration checkInterval;
    private final IQueue<Integer> replicationQueue;
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
        this.localNodeId = localNodeId;
        this.replicationFactor = replicationFactor;
        this.timeout = timeout;
        this.checkInterval = checkInterval;
        this.replicationQueue = hazelcast.getQueue(HazelcastNames.REPLICATION_QUEUE);
        this.replicaRegistry = new HazelcastReplicaRegistry(hazelcast);
    }

    @Override
    public ReplicationResult replicate(Book book) {
        replicaRegistry.register(book.bookId(), localNodeId);
        requestReplicas(book.bookId());
        ReplicationResult result = awaitReplication(book.bookId());

        if (!result.isSatisfied()) {
            throw new HazelcastAdapterException(
                    "Replication factor %d not reached for book %d; replicas=%s"
                            .formatted(replicationFactor, book.bookId(), result.replicaNodeIds())
            );
        }
        return result;
    }

    private void requestReplicas(int bookId) {
        try {
            for (int replica = 1; replica < replicationFactor; replica++) {
                replicationQueue.put(bookId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HazelcastAdapterException("Interrupted while requesting replicas", e);
        }
    }

    private ReplicationResult awaitReplication(int bookId) {
        Instant deadline = Instant.now().plus(timeout);
        List<String> nodes = replicaRegistry.nodesFor(bookId);

        while (nodes.size() < replicationFactor && Instant.now().isBefore(deadline)) {
            sleep();
            nodes = replicaRegistry.nodesFor(bookId);
        }
        return new ReplicationResult(bookId, replicationFactor, nodes);
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
