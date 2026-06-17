package com.thebiggestdata.ingestion.infrastructure.port;

import com.thebiggestdata.model.BookContent;

public interface Datalake {
    void save(int bookId, BookContent content);
    void replicate(int bookId);
}