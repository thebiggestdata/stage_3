package com.thebiggestdata.ingestion.infrastructure.adapters.web;

import com.thebiggestdata.ingestion.infrastructure.ports.old.BookDownloadStatusStore;
import com.thebiggestdata.ingestion.infrastructure.ports.old.BookStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BookStatusService implements BookStatusProvider {
    private static final Logger log = LoggerFactory.getLogger(BookStatusService.class);
    private final BookDownloadStatusStore bookDownloadLog;

    public BookStatusService(BookDownloadStatusStore bookDownloadLog) {
        this.bookDownloadLog = bookDownloadLog;
    }

    @Override
    public Map<String, Object> getBookStatus(int bookId) {
        log.info("status() - Start execution for bookId={}", bookId);
        try {
            boolean isBookAvailable = bookDownloadLog.isDownloaded(bookId);
            if (isBookAvailable) {
                return successResponse(bookId);
            } else {
                log.warn("status() - BookContent {} is not available in datalake", bookId);
                return notAvailableResponse(bookId);
            }
        } catch (Exception e) {
            log.error("status() - Error retrieving status for bookId {}: {}", bookId, e.getMessage(), e);
            return errorResponse(bookId,e.getMessage());
        } finally {
            log.info("status() - Finished execution for bookId={}", bookId);
        }
    }

    private Map<String, Object> successResponse(int bookId){
        log.info("status() - BookContent {} is available in datalake", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "available");
    }

    private Map<String, Object> notAvailableResponse(int bookId){
        return Map.of(
                "book_id", bookId,
                "status", "not_available");
    }

    private Map<String, Object> errorResponse(int bookId, String errorMessage){
        return Map.of(
                "book_id", bookId,
                "status", "error",
                "message", errorMessage);
    }
}
