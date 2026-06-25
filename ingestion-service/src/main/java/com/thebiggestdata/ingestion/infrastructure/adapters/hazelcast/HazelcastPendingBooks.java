package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.PendingBooks;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class HazelcastPendingBooks implements PendingBooks {

    private final IQueue<Integer> books;
    private final IMap<Integer, Integer> attempts;
    private final IMap<Integer, String> failures;
    private final Duration timeout;
    private final int maxAttempts;

    public HazelcastPendingBooks(HazelcastInstance hazelcast, Duration timeout, int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.books = hazelcast.getQueue(HazelcastNames.PENDING_BOOKS);
        this.attempts = hazelcast.getMap(HazelcastNames.INGESTION_ATTEMPTS);
        this.failures = hazelcast.getMap(HazelcastNames.FAILED_INGESTIONS);
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public Integer pollNext() {
        try {
            return books.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HazelcastAdapterException("Interrupted while polling pending books", e);
        }
    }

    @Override
    public void requeue(int bookId) {
        books.offer(bookId);
    }

    @Override
    public void retry(int bookId, String failureReason) {
        int attempt = incrementAttempts(bookId);
        if (attempt < maxAttempts) {
            requeue(bookId);
            return;
        }

        failures.put(bookId, failureReason == null ? "Unknown ingestion failure" : failureReason);
        attempts.remove(bookId);
    }

    @Override
    public void failPermanently(int bookId, String failureReason) {
        failures.put(bookId, failureReason == null ? "Permanent ingestion failure" : failureReason);
        attempts.remove(bookId);
    }

    private int incrementAttempts(int bookId) {
        attempts.lock(bookId);
        try {
            int attempt = attempts.getOrDefault(bookId, 0) + 1;
            attempts.put(bookId, attempt);
            return attempt;
        } finally {
            attempts.unlock(bookId);
        }
    }

    @Override
    public void complete(int bookId) {
        attempts.remove(bookId);
        failures.remove(bookId);
    }
}
