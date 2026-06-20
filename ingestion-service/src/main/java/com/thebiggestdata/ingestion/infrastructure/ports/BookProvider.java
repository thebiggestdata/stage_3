package com.thebiggestdata.ingestion.infrastructure.ports;

public interface BookProvider {
    String[] getBookContent(int bookId);
}