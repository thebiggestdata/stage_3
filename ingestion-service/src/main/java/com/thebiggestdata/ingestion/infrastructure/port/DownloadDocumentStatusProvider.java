package com.thebiggestdata.ingestion.infrastructure.port;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface DownloadDocumentStatusProvider {
    Set<Integer> loadDocuments() throws IOException;
    void saveDocs(int bookId) throws IOException;
    void registerDownload(int bookId) throws IOException;
    boolean isDownloaded(int bookId) throws IOException;
    List<Integer> getDownloadedDocs() throws IOException;
}
