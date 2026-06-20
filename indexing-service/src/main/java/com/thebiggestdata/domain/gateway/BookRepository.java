package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookText;

public interface BookRepository {
    BookText getBookContent(int bookId);
    void save(int bookId, BookText content);
}