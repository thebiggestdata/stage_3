package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.infrastructure.ports.IndexStore;
import com.thebiggestdata.search.infrastructure.ports.QueryTokenizer;
import com.thebiggestdata.search.model.IndexGeneration;
import com.thebiggestdata.search.model.SearchCriteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ContentSearchEngine {

    private final IndexStore index;
    private final QueryTokenizer tokenizer;
    private final Executor executor;

    public ContentSearchEngine(IndexStore index, QueryTokenizer tokenizer, Executor executor) {
        this.index = index;
        this.tokenizer = tokenizer;
        this.executor = executor;
    }

    public Map<Integer, Integer> find(IndexGeneration generation, SearchCriteria criteria) {
        List<String> terms = tokenizer.tokenize(criteria.query()).stream().distinct().toList();
        if (terms.isEmpty()) {
            return Map.of();
        }

        List<CompletableFuture<Map<Integer, Integer>>> searches = terms.stream()
                .map(term -> CompletableFuture.supplyAsync(() -> index.find(generation, term), executor))
                .toList();
        List<Map<Integer, Integer>> postings = searches.stream()
                .map(CompletableFuture::join)
                .toList();

        return combine(postings, requiredMatches(criteria.mode(), terms.size()));
    }

    private Map<Integer, Integer> combine(List<Map<Integer, Integer>> postings, int requiredMatches) {
        Map<Integer, Integer> frequencies = new HashMap<>();
        Map<Integer, Integer> matches = new HashMap<>();

        postings.forEach(termPostings -> termPostings.forEach((bookId, frequency) -> {
            frequencies.merge(bookId, frequency, Integer::sum);
            matches.merge(bookId, 1, Integer::sum);
        }));

        frequencies.keySet().removeIf(bookId -> matches.getOrDefault(bookId, 0) < requiredMatches);
        return Map.copyOf(frequencies);
    }

    private int requiredMatches(SearchCriteria.SearchMode mode, int termCount) {
        return mode == SearchCriteria.SearchMode.ALL_TERMS ? termCount : 1;
    }
}
