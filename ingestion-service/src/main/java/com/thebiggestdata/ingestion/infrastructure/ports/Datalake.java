package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.Book;

public interface Datalake {
    void save(Book book);
}
