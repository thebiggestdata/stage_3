package com.thebiggestdata.ingestion.infrastructure.port;

public interface DuplicationProvider {
    void duplicate(int bookId, String header, String body);
}
