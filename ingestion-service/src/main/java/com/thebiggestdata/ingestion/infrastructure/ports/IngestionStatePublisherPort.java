package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.IngestionStateEvent;

public interface IngestionStatePublisherPort {
    void publish(IngestionStateEvent event);
}
