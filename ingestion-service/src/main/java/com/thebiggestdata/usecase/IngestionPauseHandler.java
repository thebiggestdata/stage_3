package com.thebiggestdata.usecase;

import java.util.concurrent.atomic.AtomicBoolean;

public class IngestionPauseHandler {

    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    public boolean isPaused() {
        return isPaused.get();
    }

    public void pause() {
        isPaused.set(true);
    }

    public void resume() {
        isPaused.set(false);
    }
}
