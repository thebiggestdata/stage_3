package com.thebiggestdata.domain.gateway;

import java.io.IOException;
import java.util.List;

public interface BookDownloadStatusRepository {
    void registerBookDownload(int bookId) throws IOException;

    boolean isDownloaded(int bookId) throws IOException;

    List<Integer> getAllDownloadedBooks() throws IOException;
}
