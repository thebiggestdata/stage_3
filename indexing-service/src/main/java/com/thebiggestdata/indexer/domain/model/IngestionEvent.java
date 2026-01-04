package com.thebiggestdata.indexer.domain.model;

public interface IngestionEvent {
    int bookId();
    void acknowledge();
    void reject();
}
