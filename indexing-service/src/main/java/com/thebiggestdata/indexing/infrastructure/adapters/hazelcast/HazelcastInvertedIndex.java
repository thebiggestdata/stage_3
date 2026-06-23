package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HazelcastInvertedIndex implements InvertedIndex, AutoCloseable {

    private final HazelcastInstance hazelcast;
    private final ExecutorService writerPool;
    private final int concurrentWriters;

    public HazelcastInvertedIndex(HazelcastInstance hazelcast, int concurrentWriters) {
        if (concurrentWriters < 1) {
            throw new IllegalArgumentException("concurrentWriters must be positive");
        }
        this.hazelcast = hazelcast;
        this.concurrentWriters = concurrentWriters;
        this.writerPool = Executors.newFixedThreadPool(concurrentWriters, runnable -> {
            Thread thread = new Thread(runnable, "inverted-index-writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void addAll(IndexGeneration generation, List<IndexedTerm> terms) {
        IMap<String, Set<String>> index = index(generation);
        List<Map.Entry<String, Set<String>>> postings = postingsByTerm(terms).entrySet().stream().toList();
        int writers = Math.min(concurrentWriters, postings.size());
        CompletableFuture<?>[] writes = java.util.stream.IntStream.range(0, writers)
                .mapToObj(worker -> CompletableFuture.runAsync(
                        () -> addBatch(index, postings, worker, writers),
                        writerPool
                ))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(writes).join();
        } catch (CompletionException e) {
            throw new HazelcastAdapterException("Could not update inverted index", e.getCause());
        }
    }

    private Map<String, Set<String>> postingsByTerm(List<IndexedTerm> terms) {
        Map<String, Set<String>> postings = new HashMap<>();
        for (IndexedTerm term : terms) {
            postings.computeIfAbsent(term.term(), ignored -> new HashSet<>())
                    .add(encodePosting(term));
        }
        return postings;
    }

    private void addBatch(
            IMap<String, Set<String>> index,
            List<Map.Entry<String, Set<String>>> postings,
            int worker,
            int workers
    ) {
        for (int position = worker; position < postings.size(); position += workers) {
            Map.Entry<String, Set<String>> entry = postings.get(position);
            add(index, entry.getKey(), entry.getValue());
        }
    }

    private void add(IMap<String, Set<String>> index, String term, Set<String> newPostings) {
        Set<String> previous = index.putIfAbsent(term, Set.copyOf(newPostings));
        if (previous == null) {
            return;
        }

        index.lock(term);
        try {
            Set<String> merged = new HashSet<>(index.getOrDefault(term, Set.of()));
            if (merged.addAll(newPostings)) {
                index.put(term, merged);
            }
        } finally {
            index.unlock(term);
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
