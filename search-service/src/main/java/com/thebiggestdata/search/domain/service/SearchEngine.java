package com.thebiggestdata.search.service;

import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.thebiggestdata.search.domain.model.SearchResult;

public class SearchEngine {
    private final SearchBookUseCase searchBookUseCase;

    public SearchEngine(SearchBookUseCase searchBookUseCase) {
        this.searchBookUseCase = searchBookUseCase;
    }

    public SearchResult search(String query) {
        return searchBookUseCase.execute(query);
    }
}