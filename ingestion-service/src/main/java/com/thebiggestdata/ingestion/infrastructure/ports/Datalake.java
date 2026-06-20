package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookContent;

public interface Datalake {
    void save(int bookId, BookContent bookContent);
    void replicate(int bookId);
}