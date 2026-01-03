package com.thebiggestdata.ingestion.domain.service;

import com.thebiggestdata.ingestion.application.usecase.IngestBookUseCase;
import com.thebiggestdata.ingestion.domain.port.BookIdProviderPort;

public class CrawlerService {
    private final IngestBookUseCase ingestBook;
    private final BookIdProviderPort bookIdProvider;

    public CrawlerService(IngestBookUseCase ingestBook, BookIdProviderPort bookIdProvider) {
        this.ingestBook = ingestBook;
        this.bookIdProvider = bookIdProvider;
    }

    public void crawl() {
        System.out.println("Crawling...");
        while (true) {
            int id = bookIdProvider.nextBookId();
            try {
                ingestBook.ingest(id);
            } catch (Exception e) {
                System.out.println("Error crawling " + id + ": " + e.getMessage());
            }
        }
    }
}
