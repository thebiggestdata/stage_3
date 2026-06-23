package com.thebiggestdata.ingestion.infrastructure.ports;

public interface BookDownloadStatus {
    boolean isDownloaded(int bookId);
    void markAsDownloaded(int bookId);
}
