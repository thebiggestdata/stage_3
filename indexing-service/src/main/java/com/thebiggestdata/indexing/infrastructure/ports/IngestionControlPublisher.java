package com.thebiggestdata.indexing.infrastructure.ports;

public interface IngestionControlPublisher {
    void publishPause();
    void publishResume();
}
