package com.thebiggestdata.infrastructure.ports;

public interface IndexingStatusStore {
    boolean markAsIndexed(int documentId);
}