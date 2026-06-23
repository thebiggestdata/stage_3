package com.thebiggestdata.indexing.infrastructure.ports;

public interface IngestionControlPublisher {

    void pause();

    void resume();
}
