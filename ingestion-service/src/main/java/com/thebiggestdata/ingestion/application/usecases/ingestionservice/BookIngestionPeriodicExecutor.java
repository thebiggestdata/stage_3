package com.thebiggestdata.application.usecases.ingestionservice;

import com.thebiggestdata.infrastructure.ports.IngestionQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class BookIngestionPeriodicExecutor {

    private static final Logger log = LoggerFactory.getLogger(BookIngestionPeriodicExecutor.class);
    private static final long RECOVERY_LOG_INTERVAL_MS = 20_000;

    private final IngestBook ingestBookUseCase;
    private final IngestionPauseController pauseController;
    private final IngestionQueueRepository queueRepository;
    private final int bufferFactor;

    private long lastRecoveryLogTime = 0;

    public BookIngestionPeriodicExecutor(IngestBook ingestBookUseCase, IngestionPauseController pauseController,
                                         IngestionQueueRepository queueRepository, int bufferFactor) {
        this.ingestBookUseCase = ingestBookUseCase;
        this.pauseController = pauseController;
        this.queueRepository = queueRepository;
        this.bufferFactor = bufferFactor;
    }

    public void execute() {
        if (pauseController.isPaused()) {
            return;
        }

        if (shouldProcessNextBook()) {
            try {
                Integer bookId = queueRepository.pollNextBook();
                if (bookId == null) {
                    logRecoveryIfNeeded();
                } else {
                    processBook(bookId);
                }
            } catch (Exception e) {
                log.error("Error in ingestion cycle", e);
            }
        }
    }

    private boolean shouldProcessNextBook() {
        int currentSize = queueRepository.getDatalakeSize();
        int nodes = queueRepository.getIndexerNodeCount();
        if (nodes == 0) nodes = 1;
        return currentSize < (bufferFactor * nodes);
    }

    private void processBook(Integer bookId) {
        if (!queueRepository.isBookIndexed(bookId)) {
            log.info("Ingesting book: {}", bookId);
            Map<String, Object> result = ingestBookUseCase.execute(bookId);
            log.info("Result: {}\n", result);
        } else {
            log.info("Book {} is already indexed. Skipping ingestion...", bookId);
        }
    }

    private void logRecoveryIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRecoveryLogTime >= RECOVERY_LOG_INTERVAL_MS) {
            log.info("({}) [INDEXER][RECOVERY] Queue empty or rebuilding...\n", Instant.now());
            lastRecoveryLogTime = now;
        }
    }
}