package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookContent;

public interface BookContentStore {

    BookContent get(int bookId);

    void save(int bookId, BookContent content);

    void remove(int bookId);
}
