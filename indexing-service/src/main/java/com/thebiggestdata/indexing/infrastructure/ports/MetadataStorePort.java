package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookMetadata;

public interface MetadataStorePort {
    void saveMetadata(int bookId, BookMetadata metadata);
}
