package com.thebiggestdata.search.infrastructure.controller;

import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final SearchBookUseCase searchUseCase;
    private final HazelcastInstance hazelcast;
    private final AtomicInteger searchCount = new AtomicInteger(0);

    public SearchController(SearchBookUseCase searchUseCase, HazelcastInstance hazelcast) {
        this.searchUseCase = searchUseCase;
        this.hazelcast = hazelcast;
    }

    public void registerRoutes(Javalin app) {
        app.get("/search", ctx -> {
            String query = ctx.queryParam("q");

            if (query == null || query.isBlank()) {
                ctx.status(400).json(Map.of("status", "error", "message", "Missing 'q' parameter"));
                return;
            }

            logger.info("Search query: {}", query);
            var result = searchUseCase.search(query);
            searchCount.incrementAndGet();

            ctx.json(result);
        });

        app.get("/search/{book_id}", ctx -> {
            try {
                int bookId = Integer.parseInt(ctx.pathParam("book_id"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("bookId", bookId);
                response.put("found", false);
                response.put("message", "Book lookup not implemented - use search query instead");
                
                ctx.json(response);
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("status", "error", "message", "Invalid book ID"));
            }
        });

        app.get("/search/stats", ctx -> {
            MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("service", "search-service");
            stats.put("status", "running");
            stats.put("total_searches", searchCount.get());
            stats.put("unique_tokens", invertedIndex.keySet().size());
            
            ctx.json(stats);
        });
    }
}
