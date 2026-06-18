package com.thebiggestdata.infrastructure.ports;

import java.util.Map;

public interface BookListProvider {
    Map<String, Object> getBookList();
}
