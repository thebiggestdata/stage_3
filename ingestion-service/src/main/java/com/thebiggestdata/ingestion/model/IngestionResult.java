package com.thebiggestdata.ingestion.model;

public record IngestionResult(int bookId, Status status, String path, String errorMessage) {
    public enum Status {
        INGESTED,
        ALREADY_INGESTED,
        FAILED,
    }

    public static IngestionResult ingested(int bookId, String path) {
        return new IngestionResult(bookId, Status.INGESTED, path, null);
    }

    public static IngestionResult alreadyIngested(int bookId) {
        return new IngestionResult(bookId, Status.ALREADY_INGESTED, null, null);
    }

    public static IngestionResult failed(int bookId, String message) {
        return new IngestionResult(bookId, Status.FAILED, null, message);
    }
}
