package com.thebiggestdata.ingestion.infrastructure.port;

public interface ReplicationExecuter {
    void replicate(int bookId);
}
