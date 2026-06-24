package com.thebiggestdata.indexing.model;

public record IndexingResult(
        int bookId,
        Status status,
        int uniqueTermsIndexed,
        int totalTokens,
        String errorMessage
) {

    public enum Status {
        INDEXED,
        ALREADY_INDEXED,
        IN_PROGRESS,
        FAILED
    }

    public static IndexingResult indexed(int bookId, int uniqueTermsIndexed, int totalTokens) {
        return new IndexingResult(bookId, Status.INDEXED, uniqueTermsIndexed, totalTokens,null);
    }

    public static IndexingResult alreadyIndexed(int bookId) {
        return new IndexingResult(bookId, Status.ALREADY_INDEXED, 0, 0,null);
    }

    public static IndexingResult inProgress(int bookId) {
        return new IndexingResult(bookId, Status.IN_PROGRESS, 0, 0, null);
    }

    public static IndexingResult failed(int bookId, String errorMessage) {
        return new IndexingResult(bookId, Status.FAILED, 0, 0,errorMessage);
    }

    public boolean isUnrecoverableMissingContentFailure() {
        return errorMessage != null
                && errorMessage.startsWith("Book not found in live datalake or archive:");
    }
}
