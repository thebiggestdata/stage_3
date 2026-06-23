package com.thebiggestdata.ingestion.infrastructure.adapters.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicSchedulerTest {

    @Test
    void runsTheConfiguredNumberOfWorkersConcurrently() throws Exception {
        CountDownLatch workersStarted = new CountDownLatch(3);
        CountDownLatch releaseWorkers = new CountDownLatch(1);

        try (PeriodicScheduler scheduler = new PeriodicScheduler(3)) {
            scheduler.schedule(() -> {
                workersStarted.countDown();
                await(releaseWorkers);
            }, 0, 1, TimeUnit.DAYS);

            assertTrue(workersStarted.await(2, TimeUnit.SECONDS));
        } finally {
            releaseWorkers.countDown();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
