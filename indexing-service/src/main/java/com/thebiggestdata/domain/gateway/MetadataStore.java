package com.thebiggestdata.domain.gateway;

public interface MetadataStore {
    public void saveMetadata(int bookId, String header);
}
