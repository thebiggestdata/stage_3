package com.thebiggestdata.ingestion.infrastructure.adapters.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PeriodicScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PeriodicScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final int workers;

    public PeriodicScheduler(int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("workers must be positive");
        }
        this.workers = workers;
        this.scheduler = Executors.newScheduledThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable, "ingestion-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void schedule(Runnable task, long initialDelay, long period, TimeUnit unit) {
        for (int worker = 0; worker < workers; worker++) {
            scheduler.scheduleWithFixedDelay(() -> runSafely(task), initialDelay, period, unit);
        }
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Scheduled ingestion cycle failed", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
