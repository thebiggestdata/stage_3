package com.thebiggestdata.indexing.infrastructure.port;

import com.thebiggestdata.indexing.model.BookContent;

public interface DatalakeReader {
    BookContent read(int bookId);
    boolean exists(int bookId);
}