package com.searchengine.indexer.controller;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.searchengine.indexer.model.Document;
import com.searchengine.indexer.service.DatalakeClient;
import com.searchengine.indexer.service.IndexingService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexingController {

    private static final Logger logger = LoggerFactory.getLogger(IndexingController.class);
    private final IndexingService indexingService;
    private final DatalakeClient datalakeClient;
    private final HazelcastInstance hazelcast;
    private final Gson gson;
    private final AtomicInteger indexedCount = new AtomicInteger(0);

    public IndexingController(IndexingService indexingService, DatalakeClient datalakeClient, HazelcastInstance hazelcast) {
        this.indexingService = indexingService;
        this.datalakeClient = datalakeClient;
        this.hazelcast = hazelcast;
        this.gson = new Gson();
    }

    public void indexBook(Context ctx) {
        try {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            logger.info("Indexing request for book {}", bookId);

            Document document = datalakeClient.readDocument(bookId);
            indexingService.indexDocument(document);
            indexedCount.incrementAndGet();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("bookId", bookId);
            response.put("message", "Book indexed successfully");

            ctx.status(200).json(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid book ID format", e);
            ctx.status(400).json(Map.of("status", "error", "message", "Invalid book ID"));
        } catch (Exception e) {
            logger.error("Error indexing book", e);
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public void getStatus(Context ctx) {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "indexing-service");
        status.put("status", "running");
        status.put("indexed_documents", indexedCount.get());

        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");
        status.put("unique_tokens", invertedIndex.keySet().size());

        ctx.status(200).json(status);
    }

    public void getWordInfo(Context ctx) {
        try {
            String word = ctx.pathParam("word").toLowerCase();
            MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");

            Collection<Integer> bookIds = invertedIndex.get(word);

            Map<String, Object> response = new HashMap<>();
            response.put("word", word);
            response.put("bookIds", bookIds != null ? new ArrayList<>(bookIds) : List.of());
            response.put("count", bookIds != null ? bookIds.size() : 0);

            ctx.status(200).json(response);
        } catch (Exception e) {
            logger.error("Error getting word info", e);
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
