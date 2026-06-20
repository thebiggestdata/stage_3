package com.thebiggestdata.ingestion.infrastructure.ports.old;

public interface BookProvider {
    String[] getBookContent(int bookId);
} //TODO change to IngestBookPort