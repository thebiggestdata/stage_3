package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookContent;

public interface Datalake {
    void save(int bookId, BookContent content);
    void replicate(int bookId);
}