package com.thebiggestdata.domain.gateway;

import java.util.Map;

public interface BookCatalogProvider {
    Map<String, Object> getBookList();
}
