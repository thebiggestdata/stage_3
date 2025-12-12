package com.thebiggestdata.search.application;

import com.thebiggestdata.search.infrastructure.port.BookCheckProvider;
import com.thebiggestdata.search.infrastructure.port.SearchQueryProvider;
import com.thebiggestdata.search.infrastructure.port.SearchStatsProvider;
import io.javalin.http.Context;

import java.util.Map;

public class SearchController {
    private final SearchQueryProvider searchService;
    private final BookCheckProvider bookCheckService;
    private final SearchStatsProvider statsService;

    public SearchController(
            SearchQueryProvider searchService,
            BookCheckProvider bookCheckService,
            SearchStatsProvider statsService) {
        this.searchService = searchService;
        this.bookCheckService = bookCheckService;
        this.statsService = statsService;
    }

    public void search(Context ctx) {
        String query = ctx.queryParam("q");
        if (query == null || query.isBlank()) {
            ctx.status(400).json(Map.of(
                    "error", "Missing query parameter 'q'"
            ));
            return;
        }
        Map<String, Object> result = searchService.search(query);
        ctx.json(result);
    }

    public void checkBook(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = bookCheckService.check(bookId);
        ctx.json(result);
    }

    public void stats(Context ctx) {
        Map<String, Object> result = statsService.stats();
        ctx.json(result);
    }
}
