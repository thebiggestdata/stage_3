package com.thebiggestdata.indexing.infrastructure.port;

import java.util.Map;

public interface WordQueryProvider {
    Map<String, Object> query(String word);
}