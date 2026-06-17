package com.thebiggestdata.infrastructure.ports;

public interface MetadataStore {
    public void saveMetadata(int bookId, String header);
}
