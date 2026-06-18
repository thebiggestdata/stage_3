package com.thebiggestdata.infrastructure.ports;

public interface IngestionQueueRepository {
    Integer pollNextBook();
    boolean isBookIndexed(int bookId);
    int getDatalakeSize();
    int getIndexerNodeCount();
}