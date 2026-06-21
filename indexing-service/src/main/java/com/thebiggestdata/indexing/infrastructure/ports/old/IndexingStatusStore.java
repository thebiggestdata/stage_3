package com.thebiggestdata.indexing.infrastructure.ports.old;

public interface IndexingStatusStore {
    boolean markAsIndexed(int documentId);
}