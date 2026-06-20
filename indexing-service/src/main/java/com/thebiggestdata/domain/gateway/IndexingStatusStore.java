package com.thebiggestdata.domain.gateway;

public interface IndexingStatusStore {
    boolean markAsIndexed(int documentId);
}