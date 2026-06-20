package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookText;

public interface Datalake {
    void save(int bookId, BookText content);
    void replicate(int bookId);
}