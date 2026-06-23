package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.model.IndexingClaim;
import com.thebiggestdata.indexing.model.IndexGeneration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class HazelcastIndexingTracker implements IndexingTracker {

    private final String localNodeId;
    private final Duration claimLease;
    private final HazelcastInstance hazelcast;

    public HazelcastIndexingTracker(HazelcastInstance hazelcast, String localNodeId, Duration claimLease) {
        this.localNodeId = localNodeId;
        this.claimLease = claimLease;
        this.hazelcast = hazelcast;
    }

    @Override
    public IndexingClaim claim(IndexGeneration generation, int bookId) {
        ISet<Integer> indexedBooks = indexedBooks(generation);
        IMap<Integer, String> claims = claims(generation);
        if (indexedBooks.contains(bookId)) {
            return IndexingClaim.ALREADY_INDEXED;
        }

        String owner = claims.putIfAbsent(
                bookId,
                localNodeId,
                claimLease.toMillis(),
                TimeUnit.MILLISECONDS
        );
        if (owner != null) {
            return IndexingClaim.IN_PROGRESS;
        }

        if (indexedBooks.contains(bookId)) {
            claims.remove(bookId, localNodeId);
            return IndexingClaim.ALREADY_INDEXED;
        }
        return IndexingClaim.ACQUIRED;
    }

    @Override
    public void complete(IndexGeneration generation, int bookId) {
        indexedBooks(generation).add(bookId);
        release(generation, bookId);
    }

    @Override
    public void release(IndexGeneration generation, int bookId) {
        claims(generation).remove(bookId, localNodeId);
    }

    @Override
    public void clear(IndexGeneration generation) {
        indexedBooks(generation).clear();
        claims(generation).clear();
    }

    private ISet<Integer> indexedBooks(IndexGeneration generation) {
        return hazelcast.getSet(HazelcastNames.generated(HazelcastNames.INDEXED_BOOKS, generation.value()));
    }

    private IMap<Integer, String> claims(IndexGeneration generation) {
        return hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.INDEXING_IN_PROGRESS,
                generation.value()
        ));
    }
}
