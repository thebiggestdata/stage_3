package com.thebiggestdata.infrastructure.adapter.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntervalScheduler {

    private final ScheduledExecutorService scheduler;

    public IntervalScheduler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void schedule(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduler.scheduleWithFixedDelay(task, initialDelay, period, unit);
    }
    
    public void stop() {
        scheduler.shutdown();
    }
}