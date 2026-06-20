package com.thebiggestdata.ingestion.infrastructure.ports.old;

import java.util.Map;

public interface BookStatusProvider {
    Map<String, Object> getBookStatus(int bookId);
}
