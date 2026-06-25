package com.thebiggestdata.search.model;

public record SearchResult(
        int id,
        String title,
        String author,
        String language,
        Integer year,
        int frequency
) {
}
