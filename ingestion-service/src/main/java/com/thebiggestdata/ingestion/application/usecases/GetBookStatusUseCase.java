package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;

public final class GetBookStatusUseCase {

    private final BookDownloadStatus downloadStatus;

    public GetBookStatusUseCase(BookDownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public BookStatus execute(int bookId) {
        return new BookStatus(bookId, downloadStatus.isDownloaded(bookId));
    }

    public record BookStatus(int bookId, boolean available) {}
}
