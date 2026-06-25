package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import com.thebiggestdata.ingestion.infrastructure.ports.BookProvider;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookNotFoundException;

public class GutenbergBookProvider implements BookProvider {

    private final GutenbergUrlResolver urlResolver;
    private final GutenbergHttpClient httpClient;
    private final GutenbergRetryPolicy retryPolicy;
    private final GutenbergBookParser parser;

    public GutenbergBookProvider(GutenbergUrlResolver urlResolver, GutenbergHttpClient httpClient,
                                 GutenbergRetryPolicy retryPolicy, GutenbergBookParser parser) {
        this.urlResolver = urlResolver;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.parser = parser;
    }

    @Override
    public Book fetch(int bookId) {
        String url = urlResolver.resolve(bookId);
        String rawBook;
        try {
            rawBook = retryPolicy.execute(() -> httpClient.fetch(url));
        } catch (GutenbergAdapterException e) {
            if (isNotFound(e)) {
                throw new BookNotFoundException(bookId, e);
            }
            throw e;
        }
        BookContent content = parser.parse(rawBook);

        return new Book(bookId, content);
    }

    private boolean isNotFound(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof GutenbergHttpException http && http.statusCode() == 404) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
