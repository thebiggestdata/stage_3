package com.thebiggestdata.search.application.usecase;

import com.thebiggestdata.search.domain.model.SearchResult;
import com.thebiggestdata.search.domain.port.InvertedIndexReaderPort;
import com.thebiggestdata.search.domain.service.SearchEngine;

import java.util.List;

public class SearchBookUseCase {

    private final InvertedIndexReaderPort indexReader;
    private final SearchEngine searchEngine;

    public SearchBookUseCase(InvertedIndexReaderPort indexReader, SearchEngine searchEngine) {
        this.indexReader = indexReader;
        this.searchEngine = searchEngine;
    }

    public SearchResult search(String query) {
        String normalized = query.toLowerCase();
        List<Integer> bookIds = indexReader.getBookIdsForToken(normalized);
        return new SearchResult(normalized, bookIds);
    }
}
