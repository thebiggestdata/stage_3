package com.thebiggestdata.domain.gateway;

public interface IndexingStatusRepository {
    boolean markAsIndexed(int documentId);
}