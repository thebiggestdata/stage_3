package com.thebiggestdata.domain.gateway;

import java.util.Map;

public interface BookStatusReader {
    Map<String, Object> getBookStatus(int bookId);
}
