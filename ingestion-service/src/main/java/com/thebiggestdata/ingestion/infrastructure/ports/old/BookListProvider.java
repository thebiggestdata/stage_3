package com.thebiggestdata.ingestion.infrastructure.ports.old;

import java.util.Map;

public interface BookListProvider {
    Map<String, Object> getBookList();
}
