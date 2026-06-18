package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.Map;

public interface BookStatusProvider {
    Map<String, Object> getBookStatus(int bookId);
}
