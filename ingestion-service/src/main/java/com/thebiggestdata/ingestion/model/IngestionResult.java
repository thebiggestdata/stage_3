package com.thebiggestdata.ingestion.model;

public record IngestionResult(
        int bookId,
        Status status) {
    public enum Status {
        INGESTED,
        ALREADY_INGESTED,
        IN_PROGRESS,
        PAUSED
    }

    public static IngestionResult ingested(int bookId) {
        return new IngestionResult(bookId, Status.INGESTED);
    }

    public static IngestionResult alreadyIngested(int bookId) {
        return new IngestionResult(bookId, Status.ALREADY_INGESTED);
    }

    public static IngestionResult inProgress(int bookId) {
        return new IngestionResult(bookId, Status.IN_PROGRESS);
    }

    public static IngestionResult paused(int bookId) {
        return new IngestionResult(bookId, Status.PAUSED);
    }
}
