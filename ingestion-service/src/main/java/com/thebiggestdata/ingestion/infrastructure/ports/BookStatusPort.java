package com.thebiggestdata.ingestion.infrastructure.ports;

import java.util.Map;

public interface BookStatusPort {
    Map<String, Object> getBookStatus(int bookId);

}
