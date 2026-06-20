package com.thebiggestdata.domain.gateway;

import java.util.Map;

public interface BookStatusProvider {
    Map<String, Object> getBookStatus(int bookId);
}
