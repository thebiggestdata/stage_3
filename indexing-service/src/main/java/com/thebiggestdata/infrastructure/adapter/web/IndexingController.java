package com.thebiggestdata.infrastructure.adapter.web;

import com.google.gson.Gson;
import com.thebiggestdata.usecase.IndexBook;
import com.thebiggestdata.infrastructure.adapter.recovery.CoordinateRebuild;
import com.thebiggestdata.domain.entity.ReindexOutcome;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);
    private static final Gson gson = new Gson();

    private final IndexBook indexBook;
    private final CoordinateRebuild rebuildUseCase;

    public IndexingController(IndexBook indexBook, CoordinateRebuild rebuildUseCase) {
        this.indexBook = indexBook;
        this.rebuildUseCase = rebuildUseCase;
    }

    public void indexDocument(Context ctx) {
        String docIdParam = ctx.pathParam("documentId");
        log.info("WEB REQUEST: Index document {}", docIdParam);

        try {
            int documentId = Integer.parseInt(docIdParam);
            indexBook.execute(documentId);
            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", "Document indexed successfully",
                    "documentId", documentId
            )));

        } catch (NumberFormatException e) {
            log.warn("Invalid document ID format: {}", docIdParam);
            ctx.status(400).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", "Invalid ID format. Must be an integer."
            )));

        } catch (Exception e) {
            log.error("Error indexing document via Web", e);
            ctx.status(500).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage() != null ? e.getMessage() : "Internal Server Error"
            )));
        }
    }

    public void rebuild(Context ctx) {
        log.info("WEB: Request received to rebuild index");
        try {
            ReindexOutcome result = rebuildUseCase.execute();

            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", result.message()
            )));
        } catch (Exception e) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("status", "error");
            json.addProperty("message", e.getMessage());
            ctx.status(500).result(gson.toJson(json));
        }
    }

    public void health(Context ctx) {
        ctx.result(gson.toJson(Map.of("status", "healthy")));
    }
}