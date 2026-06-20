package com.thebiggestdata.ingestion.infrastructure.ports;

import java.util.Map;

public interface BookListPort {
    Map<String, Object> getBookList();
}
