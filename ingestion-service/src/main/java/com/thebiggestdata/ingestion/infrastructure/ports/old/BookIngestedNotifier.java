package com.thebiggestdata.ingestion.infrastructure.ports.old;

public interface BookIngestedNotifier {
    void notifyIngestedBook(int bookId);
}
