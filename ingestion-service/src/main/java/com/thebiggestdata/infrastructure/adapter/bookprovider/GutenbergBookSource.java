package com.thebiggestdata.infrastructure.adapter.bookprovider;

import com.thebiggestdata.domain.gateway.BookSource;

import java.io.IOException;

public class GutenbergBookSource implements BookSource {

    private final GutenbergDownloader fetcher;
    private final GutenbergClient connector;
    private final GutenbergBookTextSplitter separator;


    public GutenbergBookSource(GutenbergDownloader fetcher, GutenbergClient connector, GutenbergBookTextSplitter separator) {
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