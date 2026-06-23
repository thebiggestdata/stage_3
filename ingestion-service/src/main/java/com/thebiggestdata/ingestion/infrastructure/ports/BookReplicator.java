package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.ReplicationResult;
import com.thebiggestdata.ingestion.model.Book;

public interface BookReplicator {
    ReplicationResult replicate(Book book);
}
