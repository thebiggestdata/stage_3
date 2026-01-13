package com.thebiggestdata.ingestion.infrastructure.adapter.api;

import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ListDocumentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class ListDocumentService implements ListDocumentsProvider {
    private static final Logger log = LoggerFactory.getLogger(ListDocumentService.class);
    private final DownloadDocumentStatusProvider bookDownload;
    public ListDocumentService(DownloadDocumentStatusProvider bookDownload) {
        this.bookDownload = bookDownload;
    }

    @Override
    public Map<String, Object> list() {
        log.info("list() - Listing books in the datalake");
        try {
            List<Integer> downloadedBooks = bookDownload.getDownloadedDocs();
            return successResponse(downloadedBooks);
        } catch (Exception e) {
            log.error("list() - Error listing books: {}", e.getMessage(), e);
            return errorResponse(e.getMessage());
        } finally {
            log.info("list() - Finished execution");
        }
    }

    private Map<String, Object> successResponse(List<Integer> downloadedBooks){
        return Map.of(
                "count", downloadedBooks.size(),
                "books", downloadedBooks
        );
    }

    private Map<String, Object> errorResponse(String errorMessage){
        return Map.of(
                "status", "error",
                "message", errorMessage
        );
    }
}
