package com.thebiggestdata.indexing.model;

public record RecoveryResult(int recoveredBooks, int maxBookId, int failedBooks) {

    public boolean successful() {
        return failedBooks == 0;
    }
}
