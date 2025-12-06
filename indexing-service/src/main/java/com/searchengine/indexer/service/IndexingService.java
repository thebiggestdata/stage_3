package com.searchengine.indexer.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.searchengine.indexer.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private static final String INVERTED_INDEX_MAP = "inverted-index";

    private final HazelcastInstance hazelcast;
    private final TokenizerService tokenizerService;

    public IndexingService(HazelcastInstance hazelcast, TokenizerService tokenizerService) {
        this.hazelcast = hazelcast;
        this.tokenizerService = tokenizerService;
    }

    public void indexDocument(Document document) {
        logger.debug("Indexing document: {}", document.bookId());

        List<String> tokens = tokenizerService.tokenize(document.content());
        Map<String, List<Integer>> postings = buildPostings(document.bookId(), tokens);
        writeToIndex(postings);

        logger.debug("Document {} indexed with {} unique tokens", document.bookId(), postings.size());
    }

    private Map<String, List<Integer>> buildPostings(int bookId, List<String> tokens) {
        Map<String, List<Integer>> postings = new java.util.HashMap<>();
        for (String token : tokens) {
            postings.computeIfAbsent(token, k -> new java.util.ArrayList<>()).add(bookId);
        }
        return postings;
    }

    private void writeToIndex(Map<String, List<Integer>> postings) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap(INVERTED_INDEX_MAP);
        postings.forEach((token, bookIds) -> {
            for (Integer bookId : bookIds) {
                invertedIndex.put(token, bookId);
            }
        });
    }
}

