package com.thebiggestdata.indexing.application;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.infrastructure.port.IndexBookProvider;
import com.thebiggestdata.indexing.infrastructure.port.IndexStatusProvider;
import com.thebiggestdata.indexing.infrastructure.port.WordQueryProvider;
import io.javalin.http.Context;

import java.util.Map;

public class IndexingController {
    private final IndexBookProvider indexService;
    private final IndexStatusProvider statusService;
    private final WordQueryProvider wordQueryService;
    private static final Gson gson = new Gson();

    public IndexingController(
            IndexBookProvider indexService,
            IndexStatusProvider statusService,
            WordQueryProvider wordQueryService) {
        this.indexService = indexService;
        this.statusService = statusService;
        this.wordQueryService = wordQueryService;
    }

    public void indexBook(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = indexService.index(bookId);
        ctx.json(result);
    }

    public void status(Context ctx) {
        Map<String, Object> result = statusService.status();
        ctx.json(result);
    }

    public void queryWord(Context ctx) {
        String word = ctx.pathParam("word");
        Map<String, Object> result = wordQueryService.query(word);
        ctx.json(result);
    }
}