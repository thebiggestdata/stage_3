package com.thebiggestdata.domain.gateway;

public interface IngestionQueueRepository {
    Integer pollNextBook();
    boolean isBookIndexed(int bookId);
    int getDatalakeSize();
    int getIndexerNodeCount();
}