package com.thebiggestdata.ingestion.infrastructure.ports;

import java.io.IOException;
import java.util.List;

public interface BookDownloadStatusPort {
    boolean isDownloaded(int bookId) throws IOException;
    void markAsDownloaded(int bookId) throws IOException;
    List<Integer> getAllDownloaded() throws IOException;
}
