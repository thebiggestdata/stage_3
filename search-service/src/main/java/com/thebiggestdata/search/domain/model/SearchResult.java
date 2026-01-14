package com.thebiggestdata.search.domain.model;

import java.util.Set;

public class SearchResult {
    private final String query;
    private final Set<String> documents;
    private final int count;

    public SearchResult(String query, Set<String> documents) {
        this.query = query;
        this.documents = documents;
        this.count = documents.size();
    }

    public String getQuery() {
        return query;
    }

    public Set<String> getDocuments() {
        return documents;
    }

    public int getCount() {
        return count;
    }
}