package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.PendingBookSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HazelcastPendingBookSeeder implements PendingBookSeeder, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HazelcastPendingBookSeeder.class);
    private static final String OWNER_KEY = "owner";

    private final String localNodeId;
    private final int lastBookId;
    private final IQueue<Integer> books;
    private final IMap<String, String> initialization;
    private final ExecutorService executor;

    public HazelcastPendingBookSeeder(HazelcastInstance hazelcast, String localNodeId, int lastBookId) {
        this.localNodeId = localNodeId;
        this.lastBookId = lastBookId;
        this.books = hazelcast.getQueue(HazelcastNames.PENDING_BOOKS);
        this.initialization = hazelcast.getMap(HazelcastNames.QUEUE_INITIALIZATION);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pending-book-seeder");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void seedAfter(int maxBookId) {
        if (initialization.putIfAbsent(OWNER_KEY, localNodeId) != null) {
            return;
        }
        executor.submit(() -> seed(maxBookId + 1));
    }

    private void seed(int firstBookId) {
        int added = 0;
        for (int bookId = Math.max(1, firstBookId); bookId <= lastBookId; bookId++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (books.offer(bookId)) {
                added++;
            }
        }
        log.info("Seeded {} pending book IDs", added);
    }

    @Override
    public void reset() {
        books.clear();
        initialization.clear();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
