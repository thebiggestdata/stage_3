package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.Book;

public interface BookProvider {
    Book fetch(int bookId);
}
