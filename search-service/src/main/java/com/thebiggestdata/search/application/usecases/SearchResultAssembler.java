package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.SearchCriteria;
import com.thebiggestdata.search.model.SearchResult;

import java.util.List;
import java.util.Map;

public final class SearchResultAssembler {

    public List<SearchResult> assemble(
            Map<Integer, Integer> matches,
            Map<Integer, BookMetadata> metadata,
            SearchCriteria criteria
    ) {
        return matches.entrySet().stream()
                .filter(entry -> metadata.containsKey(entry.getKey()))
                .filter(entry -> metadata.get(entry.getKey()).matches(
                        criteria.author(),
                        criteria.language(),
                        criteria.year()
                ))
                .map(entry -> result(entry.getKey(), entry.getValue(), metadata.get(entry.getKey())))
                .toList();
    }

    private SearchResult result(int bookId, int frequency, BookMetadata metadata) {
        return new SearchResult(
                bookId,
                metadata.title(),
                metadata.author(),
                metadata.language(),
                metadata.year(),
                frequency
        );
    }
}
