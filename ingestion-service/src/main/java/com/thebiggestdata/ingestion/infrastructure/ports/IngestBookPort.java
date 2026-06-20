package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.IngestionResult;

public interface IngestBookPort {
    IngestionResult ingest(int bookId);
}
