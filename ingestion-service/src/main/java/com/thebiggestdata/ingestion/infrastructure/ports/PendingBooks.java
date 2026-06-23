package com.thebiggestdata.ingestion.infrastructure.ports;

public interface PendingBooks {

    Integer pollNext();

    void requeue(int bookId);

    void retry(int bookId, String failureReason);

    void failPermanently(int bookId, String failureReason);

    void complete(int bookId);
}
