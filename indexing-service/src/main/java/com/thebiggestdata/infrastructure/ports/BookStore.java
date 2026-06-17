package com.thebiggestdata.infrastructure.ports;

import com.thebiggestdata.model.BookContent;

public interface BookStore {
    BookContent getBookContent(int bookId);
    void save(int bookId, BookContent content);
}