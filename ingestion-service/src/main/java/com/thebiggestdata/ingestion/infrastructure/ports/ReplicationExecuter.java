package com.thebiggestdata.ingestion.infrastructure.ports;

public interface ReplicationExecuter {
    void replicate(int bookId);
}
