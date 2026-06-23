package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestedPublisher;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestionGuard;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.IngestionResult;
import com.thebiggestdata.ingestion.model.ReplicationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestBookUseCaseTest {

    @Test
    void ingestsInDurableOrderAndPublishesOnlyAfterReplication() {
        List<String> operations = new ArrayList<>();
        Status status = new Status(false, operations);
        Guard guard = new Guard(true, operations);
        Book book = new Book(42, new BookContent("header", "body"));

        IngestBookUseCase useCase = new IngestBookUseCase(
                status,
                new InMemoryIngestionState(),
                ignored -> {
                    operations.add("fetch");
                    return book;
                },
                guard,
                ignored -> operations.add("store"),
                ignored -> operations.add("datalake"),
                ignored -> {
                    operations.add("replicate");
                    return new ReplicationResult(42, 2, List.of("node-a", "node-b"));
                },
                ignored -> operations.add("publish")
        );

        IngestionResult result = useCase.execute(42);

        assertEquals(IngestionResult.Status.INGESTED, result.status());
        assertEquals(
                List.of("acquire", "fetch", "store", "datalake", "replicate", "publish", "downloaded", "release"),
                operations
        );
        assertTrue(status.downloaded);
        assertTrue(guard.released);
    }

    @Test
    void doesNothingWhenBookWasAlreadyDownloaded() {
        List<String> operations = new ArrayList<>();
        Guard guard = new Guard(true, operations);
        IngestBookUseCase useCase = useCase(new Status(true, operations), guard, operations);

        IngestionResult result = useCase.execute(7);

        assertEquals(IngestionResult.Status.ALREADY_INGESTED, result.status());
        assertTrue(operations.isEmpty());
        assertFalse(guard.released);
    }

    @Test
    void reportsInProgressWhenAnotherNodeOwnsTheLease() {
        List<String> operations = new ArrayList<>();
        Guard guard = new Guard(false, operations);
        IngestBookUseCase useCase = useCase(new Status(false, operations), guard, operations);

        IngestionResult result = useCase.execute(7);

        assertEquals(IngestionResult.Status.IN_PROGRESS, result.status());
        assertEquals(List.of("acquire"), operations);
        assertFalse(guard.released);
    }

    @Test
    void refusesNewWorkWhileIngestionIsPaused() {
        List<String> operations = new ArrayList<>();
        Guard guard = new Guard(true, operations);
        InMemoryIngestionState ingestionState = new InMemoryIngestionState();
        ingestionState.pause();
        IngestBookUseCase useCase = new IngestBookUseCase(
                new Status(false, operations),
                ingestionState,
                ignored -> new Book(7, new BookContent("", "body")),
                guard,
                ignored -> {},
                ignored -> {},
                ignored -> new ReplicationResult(7, 1, List.of("node-a")),
                ignored -> {}
        );

        IngestionResult result = useCase.execute(7);

        assertEquals(IngestionResult.Status.PAUSED, result.status());
        assertTrue(operations.isEmpty());
        assertFalse(guard.released);
    }

    @Test
    void releasesLeaseWhenAnAdapterFails() {
        List<String> operations = new ArrayList<>();
        Guard guard = new Guard(true, operations);
        IngestBookUseCase useCase = new IngestBookUseCase(
                new Status(false, operations),
                new InMemoryIngestionState(),
                ignored -> { throw new IllegalStateException("provider unavailable"); },
                guard,
                ignored -> {},
                ignored -> {},
                ignored -> new ReplicationResult(7, 1, List.of("node-a")),
                ignored -> {}
        );

        assertThrows(IllegalStateException.class, () -> useCase.execute(7));
        assertTrue(guard.released);
        assertEquals(List.of("acquire", "release"), operations);
    }

    private IngestBookUseCase useCase(Status status, Guard guard, List<String> operations) {
        return new IngestBookUseCase(
                status,
                new InMemoryIngestionState(),
                ignored -> {
                    operations.add("fetch");
                    return new Book(7, new BookContent("", "body"));
                },
                guard,
                ignored -> operations.add("store"),
                ignored -> operations.add("datalake"),
                ignored -> new ReplicationResult(7, 1, List.of("node-a")),
                ignored -> operations.add("publish")
        );
    }

    private static final class Status implements BookDownloadStatus {
        private boolean downloaded;
        private final List<String> operations;

        private Status(boolean downloaded, List<String> operations) {
            this.downloaded = downloaded;
            this.operations = operations;
        }

        @Override
        public boolean isDownloaded(int bookId) {
            return downloaded;
        }

        @Override
        public void markAsDownloaded(int bookId) {
            downloaded = true;
            operations.add("downloaded");
        }
    }

    private static final class Guard implements BookIngestionGuard {
        private final boolean acquired;
        private final List<String> operations;
        private boolean released;

        private Guard(boolean acquired, List<String> operations) {
            this.acquired = acquired;
            this.operations = operations;
        }

        @Override
        public boolean tryAcquire(int bookId) {
            operations.add("acquire");
            return acquired;
        }

        @Override
        public void release(int bookId) {
            released = true;
            operations.add("release");
        }
    }
}
