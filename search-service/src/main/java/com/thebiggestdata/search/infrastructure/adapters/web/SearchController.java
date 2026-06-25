package com.thebiggestdata.search.infrastructure.adapters.web;

import com.thebiggestdata.search.infrastructure.ports.BookSearch;
import com.thebiggestdata.search.infrastructure.ports.HealthCheck;
import com.thebiggestdata.search.model.SearchCriteria;
import com.thebiggestdata.search.model.SearchResult;
import io.javalin.http.Context;

import java.util.List;

public final class SearchController {

    private final BookSearch searchBooks;
    private final HealthCheck health;
    private final SearchRequestMapper requestMapper;
    private final SearchResponsePresenter presenter;

    public SearchController(
            BookSearch searchBooks,
            HealthCheck health,
            SearchRequestMapper requestMapper,
            SearchResponsePresenter presenter
    ) {
        this.searchBooks = searchBooks;
        this.health = health;
        this.requestMapper = requestMapper;
        this.presenter = presenter;
    }

    public void search(Context context) {
        SearchCriteria criteria = requestMapper.map(context);
        List<SearchResult> results = searchBooks.execute(criteria);
        context.json(presenter.formatSuccess(criteria, results));
    }

    public void health(Context context) {
        context.json(health.check());
    }
}
