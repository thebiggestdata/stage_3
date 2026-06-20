package com.thebiggestdata.domain.gateway;

public interface MetadataRepository {
    public void saveMetadata(int bookId, String header);
}
