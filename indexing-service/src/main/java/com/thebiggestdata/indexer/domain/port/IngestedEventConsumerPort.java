package com.thebiggestdata.indexer.domain.port;

import com.thebiggestdata.indexer.domain.model.IngestionEvent;

public interface IngestedEventConsumerPort {
    IngestionEvent consumeBookId();
}