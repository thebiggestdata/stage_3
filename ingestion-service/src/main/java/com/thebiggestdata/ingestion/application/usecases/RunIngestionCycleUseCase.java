package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.IngestionCapacity;
import com.thebiggestdata.ingestion.infrastructure.ports.IndexedBooks;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;
import com.thebiggestdata.ingestion.infrastructure.ports.PendingBooks;
import com.thebiggestdata.ingestion.model.BookNotFoundException;
import com.thebiggestdata.ingestion.model.IngestionResult;

public final class RunIngestionCycleUseCase {

    private final IngestionState pauseStatus;
    private final IngestionCapacity capacity;
    private final PendingBooks pendingBooks;
    private final IndexedBooks indexedBooks;
    private final IngestBookUseCase ingestBook;

    public RunIngestionCycleUseCase(
            IngestionState pauseStatus,
            IngestionCapacity capacity,
            PendingBooks pendingBooks,
            IndexedBooks indexedBooks,
            IngestBookUseCase ingestBook
    ) {
        this.pauseStatus = pauseStatus;
        this.capacity = capacity;
        this.pendingBooks = pendingBooks;
        this.indexedBooks = indexedBooks;
        this.ingestBook = ingestBook;
    }

    public void execute() {
        if (pauseStatus.isPaused()) {
            return;
        }

        if (!capacity.hasRoom()) {
            return;
        }

        Integer bookId = pendingBooks.pollNext();

        if (bookId == null) {
            return;
        }

        if (indexedBooks.has(bookId)) {
            pendingBooks.complete(bookId);
            return;
        }

        try {
            IngestionResult result = ingestBook.execute(bookId);
            if (result.status() == IngestionResult.Status.IN_PROGRESS
                    || result.status() == IngestionResult.Status.PAUSED) {
                pendingBooks.requeue(bookId);
                return;
            }
            pendingBooks.complete(bookId);
        } catch (BookNotFoundException e) {
            pendingBooks.failPermanently(bookId, e.getMessage());
        } catch (RuntimeException e) {
            pendingBooks.retry(bookId, e.getMessage());
            throw e;
        }
    }
}
