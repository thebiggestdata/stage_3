package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookContent;

public interface BookStore {
    BookContent getBookContent(int bookId);
    void save(int bookId, BookContent content);
}