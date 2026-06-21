package com.thebiggestdata.indexing.infrastructure.ports;

public interface IngestionControlPublisherPort {
    void publishPause();
    void publishResume();
}
