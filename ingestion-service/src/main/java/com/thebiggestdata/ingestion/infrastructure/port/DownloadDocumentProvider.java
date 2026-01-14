package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.Map;

public interface DownloadDocumentProvider {
    Map<String, Object> ingest(int bookId);
}
