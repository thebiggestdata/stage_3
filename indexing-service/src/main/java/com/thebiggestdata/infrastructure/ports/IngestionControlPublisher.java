package com.thebiggestdata.infrastructure.ports;

public interface IngestionControlPublisher {
    void publishPause();
    void publishResume();
}
