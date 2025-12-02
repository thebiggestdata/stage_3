package com.thebiggestdata.search.infrastructure.controller;

import com.thebiggestdata.search.application.usecase.SearchBookUseCase;

import io.javalin.Javalin;

public class SearchController {

    private final SearchBookUseCase searchUseCase;

    public SearchController(SearchBookUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;
    }

    public void registerRoutes(Javalin app) {
        app.get("/search", ctx -> {

            String query = ctx.queryParam("query");

            if (query == null || query.isBlank()) {
                ctx.status(400).result("Missing 'query' parameter");
                return;
            }

            var result = searchUseCase.search(query);

            ctx.json(result);
        });
    }
}
