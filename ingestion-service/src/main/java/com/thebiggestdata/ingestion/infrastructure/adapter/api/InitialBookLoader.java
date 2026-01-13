package com.thebiggestdata.ingestion.infrastructure.adapter.api;

import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InitialBookLoader {

    private static final Logger logger = LoggerFactory.getLogger(InitialBookLoader.class);
    private final DownloadDocumentProvider documentProvider;
    private final NodeIdProvider nodeIdProvider;
    private final int startId;
    private final int endId;
    private final int threads;

    public InitialBookLoader(DownloadDocumentProvider documentProvider, NodeIdProvider nodeIdProvider, int startId, int endId, int threads) {
        this.documentProvider = documentProvider;
        this.nodeIdProvider = nodeIdProvider;
        this.startId = startId;
        this.endId = endId;
        this.threads = threads;
    }

    public void loadBooks() {
        logger.info("Starting automatic book loading from {} to {} using {} threads", startId, endId, threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int bookId = startId; bookId <= endId; bookId++) {
            final int id = bookId;
            executor.submit(() -> {
                try {
                    logger.info("[{}] Downloading book {}", nodeIdProvider.nodeId(), id);
                    documentProvider.ingest(id);
                    int count = succeeded.incrementAndGet();
                    if (count % 100 == 0) {
                        logger.info("[{}] Progress: {}/{} books downloaded",
                                nodeIdProvider.nodeId(), count, endId - startId + 1);
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    logger.warn("[{}] Failed to download book {}: {}",
                            nodeIdProvider.nodeId(), id, e.getMessage());
                } finally {
                    processed.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            logger.error("Book loading interrupted", e);
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Book loading completed in {} seconds", duration / 1000);
        logger.info("Total processed: {}, Succeeded: {}, Failed: {}",
                processed.get(), succeeded.get(), failed.get());
    }
}
