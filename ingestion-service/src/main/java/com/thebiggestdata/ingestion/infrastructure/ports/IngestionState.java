package com.thebiggestdata.ingestion.infrastructure.ports;

public interface IngestionState {
    boolean isPaused();
    void pause();
    void resume();
}
