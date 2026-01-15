package com.thebiggestdata.ingestion.infrastructure.port;

public interface DocumentIngestedProvider {
    void provide(int bookId, String filepath);
}
