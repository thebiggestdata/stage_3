package com.thebiggestdata.indexing.infrastructure.ports;

public interface IndexingStatusStore {
    boolean markAsIndexed(int documentId);
}