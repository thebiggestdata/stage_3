package com.thebiggestdata.ingestion.domain.port;

import com.thebiggestdata.ingestion.domain.model.BookParts;

public interface ReplicationPort {
    void replicate(BookParts parts);
}
