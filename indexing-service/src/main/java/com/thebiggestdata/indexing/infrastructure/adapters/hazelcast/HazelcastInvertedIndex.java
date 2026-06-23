package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HazelcastInvertedIndex implements InvertedIndex, AutoCloseable {

    private final HazelcastInstance hazelcast;
    private final ExecutorService writerPool;

    public HazelcastInvertedIndex(HazelcastInstance hazelcast, int concurrentWriters) {
        if (concurrentWriters < 1) {
            throw new IllegalArgumentException("concurrentWriters must be positive");
        }
        this.hazelcast = hazelcast;
        this.writerPool = Executors.newFixedThreadPool(concurrentWriters, runnable -> {
            Thread thread = new Thread(runnable, "inverted-index-writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void addAll(IndexGeneration generation, List<IndexedTerm> terms) {
        IMap<String, Set<String>> index = index(generation);
        CompletableFuture<?>[] writes = terms.stream()
                .map(term -> CompletableFuture.runAsync(() -> add(index, term), writerPool))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(writes).join();
        } catch (CompletionException e) {
            throw new HazelcastAdapterException("Could not update inverted index", e.getCause());
        }
    }

    private void add(IMap<String, Set<String>> index, IndexedTerm term) {
        index.lock(term.term());
        try {
            Set<String> postings = new HashSet<>(index.getOrDefault(term.term(), Set.of()));
            postings.add(encodePosting(term));
            index.put(term.term(), postings);
        } finally {
            index.unlock(term.term());
        }
    }

    private String encodePosting(IndexedTerm term) {
        return term.bookId() + ":" + term.frequency();
    }

    @Override
    public void clear(IndexGeneration generation) {
        index(generation).clear();
    }

    private IMap<String, Set<String>> index(IndexGeneration generation) {
        return hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.INVERTED_INDEX,
                generation.value()
        ));
    }

    @Override
    public void close() {
        writerPool.shutdownNow();
    }
}
