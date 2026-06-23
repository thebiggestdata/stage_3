package com.thebiggestdata.indexing.model;

public record IndexedTerm(String term, int bookId, long frequency) {

    public IndexedTerm {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("term must not be blank");
        }
        if (bookId < 1) {
            throw new IllegalArgumentException("bookId must be positive");
        }
        if (frequency < 1) {
            throw new IllegalArgumentException("frequency must be positive");
        }
    }
}
