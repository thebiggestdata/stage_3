package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.Map;

public interface DocumentStatusProvider {
    Map<String, Object> status(int bookId);
}
