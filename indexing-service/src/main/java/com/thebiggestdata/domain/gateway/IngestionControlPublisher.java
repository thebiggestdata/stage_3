package com.thebiggestdata.domain.gateway;

public interface IngestionControlPublisher {
    void publishPause();
    void publishResume();
}
