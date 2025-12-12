package com.thebiggestdata.search.infrastructure.port;

import java.util.Map;

public interface SearchQueryProvider {
    Map<String, Object> search(String query);
}