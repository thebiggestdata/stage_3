package com.thebiggestdata.search.domain.service;

import com.thebiggestdata.search.domain.model.SearchResult;

import java.util.List;
import java.util.Map;

public class SearchEngine {

    public SearchResult search(String query, Map<String, List<Integer>> reversedIndex) {
        String token = query.toLowerCase();

        List<Integer> results = reversedIndex.getOrDefault(token, List.of());

        return new SearchResult(token, results);
    }
}
