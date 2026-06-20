package com.thebiggestdata.domain.gateway;

public interface IngestionSignalEmitter {
    void publishPause();
    void publishResume();
}
