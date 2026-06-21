package com.thebiggestdata.indexing.model;

public record IndexingResult(int BookId, Status status, int uniqueTermsIndexed, int totalTokens, String errorMessage) {

    public enum Status {
        INDEXED,
        ALREADY_INDEXED,
        FAILED
    }

    public static IndexingResult indexed(int bookId, int uniqueTermsIndexed, int totalTokens) {
        return new IndexingResult(bookId, Status.INDEXED, uniqueTermsIndexed, totalTokens,null);
    }

    public static IndexingResult alreadyIndexed(int bookId) {
        return new IndexingResult(bookId, Status.ALREADY_INDEXED, 0, 0,null);
    }

    public static IndexingResult failed(int bookId, String errorMessage) {
        return new IndexingResult(bookId, Status.FAILED, 0, 0,errorMessage);
    }

}
