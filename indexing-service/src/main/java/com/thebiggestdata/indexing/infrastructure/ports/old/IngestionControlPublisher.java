package com.thebiggestdata.indexing.infrastructure.ports.old;

public interface IngestionControlPublisher {
    void publishPause();
    void publishResume();
}
