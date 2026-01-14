package com.thebiggestdata.search.application.usecase;

import com.thebiggestdata.search.domain.model.SearchResult;
import com.thebiggestdata.search.domain.port.InvertedIndexReaderPort;

import java.util.HashSet;
import java.util.Set;

public class SearchBookUseCase {
    private final InvertedIndexReaderPort invertedIndexReader;

    public SearchBookUseCase(InvertedIndexReaderPort invertedIndexReader) {
        this.invertedIndexReader = invertedIndexReader;
    }

    public SearchResult execute(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResult(query, new HashSet<>());
        }

        String[] terms = query.toLowerCase().trim().split("\\s+");

        if (terms.length == 0) {
            return new SearchResult(query, new HashSet<>());
        }

        Set<String> result = new HashSet<>(invertedIndexReader.getDocumentsForTerm(terms[0]));

        for (int i = 1; i < terms.length; i++) {
            Set<String> docsForTerm = invertedIndexReader.getDocumentsForTerm(terms[i]);
            result.retainAll(docsForTerm);
        }

        return new SearchResult(query, result);
    }
}