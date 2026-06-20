package com.thebiggestdata.indexing.infrastructure.ports;

public interface MetadataStore {
    public void saveMetadata(int bookId, String header);
}
