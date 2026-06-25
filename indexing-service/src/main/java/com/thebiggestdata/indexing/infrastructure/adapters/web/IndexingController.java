package com.thebiggestdata.indexing.infrastructure.adapters.web;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.application.usecases.IndexBookUseCase;
import com.thebiggestdata.indexing.application.usecases.RebuildIndexUseCase;
import com.thebiggestdata.indexing.model.IndexingResult;
import io.javalin.http.Context;

public final class IndexingController {

    private final IndexBookUseCase indexBook;
    private final RebuildIndexUseCase rebuildIndex;
    private final Gson gson;

    public IndexingController(IndexBookUseCase indexBook, RebuildIndexUseCase rebuildIndex, Gson gson) {
        this.indexBook = indexBook;
        this.rebuildIndex = rebuildIndex;
        this.gson = gson;
    }

    public void index(Context context) {
        IndexingResult result = indexBook.execute(positiveBookId(context));
        context.status(statusFor(result));
        context.result(gson.toJson(result));
    }

    public void rebuild(Context context) {
        context.result(gson.toJson(rebuildIndex.execute()));
    }

    private int positiveBookId(Context context) {
        int bookId = Integer.parseInt(context.pathParam("book_id"));
        if (bookId < 1) {
            throw new IllegalArgumentException("book_id must be positive");
        }
        return bookId;
    }

    private int statusFor(IndexingResult result) {
        return switch (result.status()) {
            case INDEXED, ALREADY_INDEXED -> 200;
            case IN_PROGRESS -> 202;
            case FAILED -> 500;
        };
    }
}
