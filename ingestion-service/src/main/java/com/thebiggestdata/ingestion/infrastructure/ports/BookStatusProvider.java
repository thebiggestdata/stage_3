package com.thebiggestdata.ingestion.infrastructure.ports;

import java.util.Map;

public interface BookStatusProvider {
    Map<String, Object> getBookStatus(int bookId);
}
