package com.thebiggestdata.ingestion.infrastructure.ports;

public interface BookIngestedNotifier {
    void notifyIngestedBook(int bookId);
}
