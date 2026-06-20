package com.thebiggestdata.ingestion.model;

public record IngestionResult(int bookId, Status status, String path, String errorMessage) {
    public enum Status {
        INGESTED,
        PRESENT,
        FAILED,
    }

    public static IngestionResult ingested(int bookId, String path) {
        return new IngestionResult(bookId, Status.INGESTED, path, null);
    }

    public static IngestionResult present(int bookId) {
        return new IngestionResult(bookId, Status.PRESENT, null, null);
    }

    public static IngestionResult failed(int bookId, String message) {
        return new IngestionResult(bookId, Status.FAILED, null, message);
    }
}
