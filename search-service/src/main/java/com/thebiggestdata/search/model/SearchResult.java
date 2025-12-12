package com.thebiggestdata.search.model;

import java.util.List;

public record SearchResult(
        String query,
        List<Integer> bookIds,
        int termsMatched
) {
}