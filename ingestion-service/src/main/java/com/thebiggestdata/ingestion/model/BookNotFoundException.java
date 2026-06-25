package com.thebiggestdata.ingestion.model;

public final class BookNotFoundException extends RuntimeException {

    private final int bookId;

    public BookNotFoundException(int bookId, Throwable cause) {
        super("Book %d does not exist in Gutenberg".formatted(bookId), cause);
        this.bookId = bookId;
    }

    public int bookId() {
        return bookId;
    }
}
