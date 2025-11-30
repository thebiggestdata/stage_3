package com.thebiggestdata.indexer.domain.port;

import com.thebiggestdata.indexer.domain.model.RawDocument;

public interface DatalakeReadPort {
    RawDocument read(int bookId, String bodyPath);
}
