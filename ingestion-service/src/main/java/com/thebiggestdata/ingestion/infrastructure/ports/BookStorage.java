package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.Book;

public interface BookStorage {
    void save(Book book);

}
