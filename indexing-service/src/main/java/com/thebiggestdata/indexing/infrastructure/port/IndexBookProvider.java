package com.thebiggestdata.indexing.infrastructure.port;

import java.util.Map;

public interface IndexBookProvider {
    Map<String, Object> index(int bookId);
}