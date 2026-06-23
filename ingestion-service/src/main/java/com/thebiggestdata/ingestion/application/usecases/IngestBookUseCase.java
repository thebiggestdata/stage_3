package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.ports.BookProvider;
import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.infrastructure.ports.Datalake;
import com.thebiggestdata.ingestion.infrastructure.ports.BookReplicator;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestedPublisher;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestionGuard;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;
import com.thebiggestdata.ingestion.model.IngestionResult;
import com.thebiggestdata.ingestion.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngestBookUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestBookUseCase.class);

    private final BookDownloadStatus downloadStatus;
    private final IngestionState ingestionState;
    private final BookIngestionGuard ingestionGuard;
    private final BookProvider bookProvider;
    private final BookStorage bookStorage;
    private final Datalake datalake;
    private final BookReplicator bookReplicator;
    private final BookIngestedPublisher publisher;
    private final String localNodeId;

    public IngestBookUseCase(
            BookDownloadStatus downloadStatus,
            IngestionState ingestionState,
            BookProvider bookProvider,
            BookIngestionGuard ingestionGuard,
            BookStorage bookStorage,
            Datalake datalake,
            BookReplicator bookReplicator,
            BookIngestedPublisher publisher,
            String localNodeId
    ) {
        this.downloadStatus = downloadStatus;
        this.ingestionState = ingestionState;
        this.ingestionGuard = ingestionGuard;
        this.bookProvider = bookProvider;
        this.bookStorage = bookStorage;
        this.datalake = datalake;
        this.bookReplicator = bookReplicator;
        this.publisher = publisher;
        this.localNodeId = localNodeId;
    }

    public IngestionResult execute(int bookId) {
        if (downloadStatus.isDownloaded(bookId)) {
            return IngestionResult.alreadyIngested(bookId);
        }

        if (ingestionState.isPaused()) {
            return IngestionResult.paused(bookId);
        }

        if (!ingestionGuard.tryAcquire(bookId)) {
            return IngestionResult.inProgress(bookId);
        }

        try {
            if (downloadStatus.isDownloaded(bookId)) {
                return IngestionResult.alreadyIngested(bookId);
            }

            if (ingestionState.isPaused()) {
                return IngestionResult.paused(bookId);
            }

            Book book = bookProvider.fetch(bookId);

            bookStorage.save(book);
            datalake.save(book);
            bookReplicator.replicate(book);
            publisher.publish(new BookIngestedEvent(bookId, localNodeId));
            downloadStatus.markAsDownloaded(bookId);

            log.info("INGESTED bookId={} nodeId={}", bookId, localNodeId);
            return IngestionResult.ingested(bookId);
        } finally {
            ingestionGuard.release(bookId);
        }
    }
}
