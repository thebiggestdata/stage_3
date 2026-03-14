package org.example.indexing;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller for the Indexing Service.
 *
 * Endpoints:
 *   GET  /api/health              – liveness probe
 *   POST /api/index?path=&lt;path&gt;  – manually trigger indexing of a body file
 */
public class IndexingController {

    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    private final IndexingService indexingService;

    public IndexingController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    public void register(Javalin app) {
        app.get("/api/health", this::health);
        app.post("/api/index", this::indexBook);
    }

    private void health(Context ctx) {
        ctx.json(Map.of("status", "UP", "service", "indexing"));
    }

    private void indexBook(Context ctx) {
        String path = ctx.queryParam("path");
        if (path == null || path.isBlank()) {
            ctx.status(400).json(Map.of("error", "path query parameter is required"));
            return;
        }
        try {
            indexingService.indexBook(path);
            ctx.json(Map.of("status", "indexed", "path", path));
        } catch (IOException e) {
            log.error("Failed to index {}", path, e);
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
