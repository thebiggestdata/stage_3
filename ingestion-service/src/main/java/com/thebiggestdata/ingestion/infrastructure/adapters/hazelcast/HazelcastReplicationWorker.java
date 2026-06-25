package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HazelcastReplicationWorker implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HazelcastReplicationWorker.class);

    private final String localNodeId;
    private final HazelcastDatalake datalake;
    private final BookStorage storage;
    private final IQueue<Integer> queue;
    private final HazelcastReplicaRegistry replicaRegistry;
    private final ExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean();

    public HazelcastReplicationWorker(
            HazelcastInstance hazelcast,
            String localNodeId,
            HazelcastDatalake datalake,
            BookStorage storage
    ) {
        this.localNodeId = localNodeId;
        this.datalake = datalake;
        this.storage = storage;
        this.replicaRegistry = new HazelcastReplicaRegistry(hazelcast);
        this.queue = hazelcast.getQueue(HazelcastNames.replicationQueueFor(localNodeId));
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "book-replication-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            executor.submit(this::run);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Integer bookId = queue.poll(1, TimeUnit.SECONDS);
                if (bookId != null) {
                    replicate(bookId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.error("Could not replicate queued book", e);
            }
        }
    }

    private void replicate(int bookId) throws InterruptedException {
        if (replicaRegistry.contains(bookId, localNodeId)) {
            log.debug("Replica already stored locally; skipping bookId={} nodeId={}", bookId, localNodeId);
            return;
        }

        String sourceNodeId = replicaRegistry.nodesFor(bookId).stream()
                .filter(nodeId -> !nodeId.equals(localNodeId))
                .findFirst()
                .orElse("unknown");
        BookContent content = datalake.get(bookId);
        storage.save(new Book(bookId, content));
        replicaRegistry.register(bookId, localNodeId);
        log.info("REPLICA_STORED bookId={} source={} target={}", bookId, sourceNodeId, localNodeId);
    }
}
