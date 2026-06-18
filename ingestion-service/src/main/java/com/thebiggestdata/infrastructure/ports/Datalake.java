package com.thebiggestdata.infrastructure.ports;

import com.thebiggestdata.model.BookContent;

public interface Datalake {
    void save(int bookId, BookContent content);
    void replicate(int bookId);
}