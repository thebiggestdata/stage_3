package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookContent;

public interface BookContentReaderPort {
    BookContent getBook(int bookId);
}
