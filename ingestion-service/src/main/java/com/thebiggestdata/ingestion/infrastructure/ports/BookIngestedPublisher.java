package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookIngestedEvent;

public interface BookIngestedPublisher {
    void publish(BookIngestedEvent event);
}
