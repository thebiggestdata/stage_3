package com.thebiggestdata.ingestion.infrastructure.port;

public interface BookProvider {
    String[] getBookContent(int bookId);
}