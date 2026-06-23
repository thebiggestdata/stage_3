package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

public class GutenbergUrlResolver {
    private static final String URL_TEMPLATE = "https://www.gutenberg.org/cache/epub/%d/pg%d.txt";

    public String resolve(int bookId) {
        return URL_TEMPLATE.formatted(bookId, bookId);
    }
}
