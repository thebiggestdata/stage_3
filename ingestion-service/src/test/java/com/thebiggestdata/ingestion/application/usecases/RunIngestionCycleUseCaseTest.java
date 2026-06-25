package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestionGuard;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;
import com.thebiggestdata.ingestion.infrastructure.ports.PendingBooks;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.BookNotFoundException;
import com.thebiggestdata.ingestion.model.ReplicationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunIngestionCycleUseCaseTest {

    @Test
    void consumesAndCompletesOneEligibleBook() {
        AtomicInteger fetchedBook = new AtomicInteger();
        PendingBooksStub pending = new PendingBooksStub(23);
        RunIngestionCycleUseCase cycle = new RunIngestionCycleUseCase(
                state(false),
                () -> true,
                pending,
                ignored -> false,
                ingestionUseCase(fetchedBook)
        );

        cycle.execute();

        assertEquals(23, fetchedBook.get());
        assertEquals(23, pending.completedBook);
    }

    @Test
    void leavesQueueUntouchedWhilePausedOrAtCapacity() {
        PendingBooksStub pausedPending = new PendingBooksStub(1);
        PendingBooksStub fullPending = new PendingBooksStub(1);
        IngestBookUseCase ingestBook = ingestionUseCase(new AtomicInteger());

        new RunIngestionCycleUseCase(
                state(true),
                () -> true,
                pausedPending,
                ignored -> false,
                ingestBook
        ).execute();
        new RunIngestionCycleUseCase(
                state(false),
                () -> false,
                fullPending,
                ignored -> false,
                ingestBook
        ).execute();

        assertEquals(0, pausedPending.polls);
        assertEquals(0, fullPending.polls);
    }

    @Test
    void completesBooksAlreadyPresentInTheIndex() {
        PendingBooksStub pending = new PendingBooksStub(23);
        AtomicInteger fetchedBook = new AtomicInteger();
        RunIngestionCycleUseCase cycle = new RunIngestionCycleUseCase(
                state(false),
                () -> true,
                pending,
                ignored -> true,
                ingestionUseCase(fetchedBook)
        );

        cycle.execute();

        assertEquals(0, fetchedBook.get());
        assertEquals(23, pending.completedBook);
    }

    @Test
    void retriesBookAfterAnIngestionFailure() {
        PendingBooksStub pending = new PendingBooksStub(23);
        IngestBookUseCase failingUseCase = ingestionUseCase(
                new AtomicInteger(),
                new IllegalStateException("broker unavailable")
        );
        RunIngestionCycleUseCase cycle = new RunIngestionCycleUseCase(
                state(false),
                () -> true,
                pending,
                ignored -> false,
                failingUseCase
        );

        assertThrows(IllegalStateException.class, cycle::execute);
        assertEquals(23, pending.retriedBook);
        assertEquals("broker unavailable", pending.failureReason);
    }

    @Test
    void recordsMissingBooksWithoutRetryingThem() {
        PendingBooksStub pending = new PendingBooksStub(404);
        RunIngestionCycleUseCase cycle = new RunIngestionCycleUseCase(
                state(false),
                () -> true,
                pending,
                ignored -> false,
                ingestionUseCase(
                        new AtomicInteger(),
                        new BookNotFoundException(404, null)
                )
        );

        cycle.execute();

        assertEquals(404, pending.permanentlyFailedBook);
        assertEquals(0, pending.retriedBook);
    }

    private IngestionState state(boolean paused) {
        return new IngestionState() {
            private boolean value = paused;

            @Override public boolean isPaused() { return value; }
            @Override public void pause() { value = true; }
            @Override public void resume() { value = false; }
        };
    }

    private IngestBookUseCase ingestionUseCase(AtomicInteger fetchedBook) {
        return ingestionUseCase(fetchedBook, null);
    }

    private IngestBookUseCase ingestionUseCase(AtomicInteger fetchedBook, RuntimeException failure) {
        BookDownloadStatus status = new BookDownloadStatus() {
            @Override public boolean isDownloaded(int bookId) { return false; }
            @Override public void markAsDownloaded(int bookId) {}
        };
        BookIngestionGuard guard = new BookIngestionGuard() {
            @Override public boolean tryAcquire(int bookId) { return true; }
            @Override public void release(int bookId) {}
        };
        return new IngestBookUseCase(
                status,
                new InMemoryIngestionState(),
                bookId -> {
                    fetchedBook.set(bookId);
                    if (failure != null) {
                        throw failure;
                    }
                    return new Book(bookId, new BookContent("", "body"));
                },
                guard,
                ignored -> {},
                ignored -> {},
                book -> new ReplicationResult(book.bookId(), 1, List.of("node-a")),
                ignored -> {},
                "test-node"
        );
    }

    private static final class PendingBooksStub implements PendingBooks {
        private final Integer nextBook;
        private int polls;
        private int completedBook;
        private int retriedBook;
        private int permanentlyFailedBook;
        private String failureReason;

        private PendingBooksStub(Integer nextBook) {
            this.nextBook = nextBook;
        }

        @Override public Integer pollNext() { polls++; return nextBook; }
        @Override public void requeue(int bookId) {}
        @Override public void retry(int bookId, String failureReason) {
            this.retriedBook = bookId;
            this.failureReason = failureReason;
        }
        @Override public void failPermanently(int bookId, String failureReason) {
            this.permanentlyFailedBook = bookId;
            this.failureReason = failureReason;
        }
        @Override public void complete(int bookId) { completedBook = bookId; }
    }
}
