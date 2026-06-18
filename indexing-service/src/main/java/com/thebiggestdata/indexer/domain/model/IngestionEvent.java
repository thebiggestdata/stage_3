package com.thebiggestdata.indexer.domain.model;

public interface IngestionEvent {
    int bookId();
    String path();
    void acknowledge();
    void reject();
}
