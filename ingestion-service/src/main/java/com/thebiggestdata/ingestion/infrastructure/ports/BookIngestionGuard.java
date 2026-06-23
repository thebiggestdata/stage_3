package com.thebiggestdata.ingestion.infrastructure.ports;

public interface BookIngestionGuard {

    boolean tryAcquire(int bookId);

    void release(int bookId);
}
