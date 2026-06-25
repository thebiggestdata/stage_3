package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexingClaim;
import com.thebiggestdata.indexing.model.IndexGeneration;

public interface IndexingTracker {

    IndexingClaim claim(IndexGeneration generation, int bookId);

    void complete(IndexGeneration generation, int bookId);

    void release(IndexGeneration generation, int bookId);

    void clear(IndexGeneration generation);
}
