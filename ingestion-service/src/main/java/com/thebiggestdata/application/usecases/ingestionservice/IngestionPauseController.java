package com.thebiggestdata.application.usecases.ingestionservice;

import java.util.concurrent.atomic.AtomicBoolean;

public class IngestionPauseController {

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
