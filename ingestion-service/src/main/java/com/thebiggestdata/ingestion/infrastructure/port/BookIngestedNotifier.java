package com.thebiggestdata.ingestion.infrastructure.port;

public interface BookIngestedNotifier {
    void notifyIngestedBook(int bookId);
}
