package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.Map;

public interface BookListProvider {
    Map<String, Object> getBookList();
}
