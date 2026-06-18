package com.thebiggestdata.ingestion.infrastructure.adapter.api;

import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQIngestedBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.FetchGutenbergBook;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.StorageDocDate;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.Map;

public class IngestDocumentService implements DownloadDocumentProvider {
    private static final Logger log = LoggerFactory.getLogger(IngestDocumentService.class);
    private final DownloadDocumentStatusProvider bookDownloadLog;
    private final StorageDocDate storageDate;
    private final ActiveMQIngestedBookProvider bookIngestedNotifier;

    public IngestDocumentService(DownloadDocumentStatusProvider bookDownloadLog, StorageDocDate storageDate, ActiveMQIngestedBookProvider bookIngestedNotifier) {
        this.bookDownloadLog = bookDownloadLog;
        this.storageDate = storageDate;
        this.bookIngestedNotifier = bookIngestedNotifier;
    }

    @Override
    public Map<String, Object> ingest(int bookId) {
        log.info("ingest() - Start processing bookId={}", bookId);
        try {
            if (bookDownloadLog.isDownloaded(bookId)) return alreadyDownloadedResponse(bookId);
            String response = fetchBook(bookId);

            Path savedPath = storageDate.store(bookId, response);

            bookDownloadLog.registerDownload(bookId);

            this.bookIngestedNotifier.provide(bookId, savedPath.toString());

            log.info("Event published for book {} with path {}", bookId, savedPath);
            return successResponse(bookId, savedPath);
        } catch (Exception e) {
            return errorResponse(bookId, e);
        } finally {
            log.info("ingest() - Finished processing bookId={}", bookId);
        }
    }

    private String fetchBook(int bookId) throws Exception {
        FetchGutenbergBook fetch = new FetchGutenbergBook();
        return fetch.fetch(bookId);
    }

    private Map<String, Object> alreadyDownloadedResponse(int bookId) {
        log.warn("ingest() - Book {} is already downloaded, skipping ingestion", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "already_downloaded",
                "message", "Book already exists in datalake"
        );
    }

    private Map<String, Object> successResponse(int bookId, Path savedPath) {
        log.info("ingest() - Book {} downloaded and saved at {}", bookId, savedPath);
        return Map.of(
                "book_id", bookId,
                "status", "downloaded",
                "path", savedPath.toString().replace("\\", "/")
        );
    }

    private Map<String, Object> errorResponse(int bookId, Exception e) {
        log.error("ingest() - Error processing bookId {}: {}", bookId, e.getMessage(), e);
        return Map.of(
                "book_id", bookId,
                "status", "error",
                "message", e.getMessage()
        );
    }
}