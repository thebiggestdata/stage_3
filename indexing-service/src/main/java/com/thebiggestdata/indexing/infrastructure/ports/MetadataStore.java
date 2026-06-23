package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexGeneration;

public interface MetadataStore {

    void save(IndexGeneration generation, int bookId, BookMetadata metadata);

    void clear(IndexGeneration generation);
}
