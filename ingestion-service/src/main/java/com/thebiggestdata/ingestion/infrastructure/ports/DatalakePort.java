package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.ReplicationResult;

public interface DatalakePort {
    void save(int bookId, BookContent bookContent);

    ReplicationResult replicate(int bookId, int replicationFactor);
}
