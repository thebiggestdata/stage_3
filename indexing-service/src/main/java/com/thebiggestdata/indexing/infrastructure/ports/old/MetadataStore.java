package com.thebiggestdata.indexing.infrastructure.ports.old;

public interface MetadataStore {
    public void saveMetadata(int bookId, String header);
}
