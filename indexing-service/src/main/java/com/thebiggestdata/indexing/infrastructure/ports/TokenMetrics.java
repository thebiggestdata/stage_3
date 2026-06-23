package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexGeneration;

public interface TokenMetrics {

    void record(IndexGeneration generation, int bookId, int tokenCount);

    void clear(IndexGeneration generation);
}
