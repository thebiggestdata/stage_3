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

public class IngestBookUseCase {

    private final BookDownloadStatus downloadStatus;
    private final IngestionState ingestionState;
    private final BookIngestionGuard ingestionGuard;
    private final BookProvider bookProvider;
    private final BookStorage bookStorage;
    private final Datalake datalake;
    private final BookReplicator bookReplicator;
    private final BookIngestedPublisher publisher;

    public IngestBookUseCase(
            BookDownloadStatus downloadStatus,
            IngestionState ingestionState,
            BookProvider bookProvider,
            BookIngestionGuard ingestionGuard,
            BookStorage bookStorage,
            Datalake datalake,
            BookReplicator bookReplicator,
            BookIngestedPublisher publisher
    ) {
        this.downloadStatus = downloadStatus;
        this.ingestionState = ingestionState;
        this.ingestionGuard = ingestionGuard;
        this.bookProvider = bookProvider;
        this.bookStorage = bookStorage;
        this.datalake = datalake;
        this.bookReplicator = bookReplicator;
        this.publisher = publisher;
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
            publisher.publish(new BookIngestedEvent(bookId));
            downloadStatus.markAsDownloaded(bookId);

            return IngestionResult.ingested(bookId);
        } finally {
            ingestionGuard.release(bookId);
        }
    }
}
