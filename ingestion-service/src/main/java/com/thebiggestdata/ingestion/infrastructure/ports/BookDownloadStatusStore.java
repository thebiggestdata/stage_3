package com.thebiggestdata.ingestion.infrastructure.ports;

import java.io.IOException;
import java.util.List;

public interface BookDownloadStatusStore {
    void registerBookDownload(int bookId) throws IOException;

    boolean isDownloaded(int bookId) throws IOException;

    List<Integer> getAllDownloadedBooks() throws IOException;
}
