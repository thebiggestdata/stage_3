package com.thebiggestdata.indexing.model;

public record Book(int bookId, BookContent content) {

    public Book {
        if (bookId < 1) {
            throw new IllegalArgumentException("bookId must be positive");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}
