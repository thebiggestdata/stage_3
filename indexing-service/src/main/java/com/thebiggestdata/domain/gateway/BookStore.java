package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookContent;

public interface BookStore {
    BookContent getBookContent(int bookId);
    void save(int bookId, BookContent content);
}