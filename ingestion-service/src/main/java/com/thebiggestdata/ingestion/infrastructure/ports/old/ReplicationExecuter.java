package com.thebiggestdata.ingestion.infrastructure.ports.old;

public interface ReplicationExecuter {
    void replicate(int bookId);
}
