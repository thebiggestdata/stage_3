package com.thebiggestdata.ingestion.infrastructure.port;

public interface IngestionQueueRepository {
    Integer pollNextBook();
    boolean isBookIndexed(int bookId);
    int getDatalakeSize();
    int getIndexerNodeCount();
}