package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import com.thebiggestdata.ingestion.infrastructure.ports.old.BookProvider;

import java.io.IOException;

public class GutenbergBookProvider implements BookProvider {

    private final GutenbergFetch fetcher;
    private final GutenbergConnection connector;
    private final GutenbergBookContentSeparator separator;


    public GutenbergBookProvider(GutenbergFetch fetcher, GutenbergConnection connector, GutenbergBookContentSeparator separator) {
        this.fetcher = fetcher;
        this.connector = connector;
        this.separator = separator;
    }

    @Override
    public String[] getBookContent(int bookId) {
        try {
            var connection = connector.createConnection(bookId);
            String rawContent = fetcher.fetchBook(connection);
            return separator.separateContent(rawContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch book from Gutenberg: " + bookId, e);
        }
    }
}