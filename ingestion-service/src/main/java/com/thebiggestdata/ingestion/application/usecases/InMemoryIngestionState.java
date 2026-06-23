package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;

import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemoryIngestionState implements IngestionState {

    private final AtomicBoolean paused = new AtomicBoolean(false);

    @Override
    public boolean isPaused() {
        return paused.get();
    }

    @Override
    public void pause() {
        paused.set(true);
    }

    @Override
    public void resume() {
        paused.set(false);
    }
}