package com.thebiggestdata.search.model;

public record SearchCriteria(String query, String author, String language, Integer year, SearchMode mode) {

    public SearchCriteria {
        if (mode == null) {
            mode = SearchMode.ALL_TERMS;
        }
    }

    public enum SearchMode{
        ALL_TERMS,
        ANY_TERM
    }

    public SearchCriteria(String query, String author, String language, Integer year) {
        this(query, author, language, year, SearchMode.ALL_TERMS);
    }

}
