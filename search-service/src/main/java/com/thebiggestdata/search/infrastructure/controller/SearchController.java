package com.thebiggestdata.search.infrastructure.controller;

import io.javalin.Javalin;
import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.thebiggestdata.search.domain.model.SearchResult;

public class SearchController {

    private final SearchBookUseCase searchUseCase;

    public SearchController(Javalin app, SearchBookUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;

        app.get("/search", ctx -> {
            String query = ctx.queryParam("q");

            if (query == null || query.isBlank()) {
                ctx.status(400).result("Missing query param 'q'");
                return;
            }

            SearchResult result = searchUseCase.execute(query);
            ctx.json(result);
        });
    }
}