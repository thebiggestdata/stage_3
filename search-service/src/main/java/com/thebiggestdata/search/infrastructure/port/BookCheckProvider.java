package com.thebiggestdata.search.infrastructure.port;

import java.util.Map;

public interface BookCheckProvider {
    Map<String, Object> check(int bookId);
}