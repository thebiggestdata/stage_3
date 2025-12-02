package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.model.BookParts;
import com.thebiggestdata.ingestion.domain.port.ReplicationPort;

public class NoOpReplicationAdapter implements ReplicationPort {

    @Override
    public void replicate(BookParts parts) {
    }
}
