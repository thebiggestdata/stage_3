package com.thebiggestdata.domain.gateway;

public interface IngestionQueueStore {
    Integer pollNextBook();
    boolean isBookIndexed(int bookId);
    int getDatalakeSize();
    int getIndexerNodeCount();
}