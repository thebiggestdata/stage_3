package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexingResult;

public interface IndexBookPort {
    IndexingResult index(int bookId);
}
