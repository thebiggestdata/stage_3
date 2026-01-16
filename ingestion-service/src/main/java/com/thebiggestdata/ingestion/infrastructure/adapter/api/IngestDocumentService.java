package com.thebiggestdata.ingestion.infrastructure.adapter.api;

import com.thebiggestdata.ingestion.infrastructure.port.*;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.FetchGutenbergBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class IngestDocumentService implements DownloadDocumentProvider {
    private static final Logger log = LoggerFactory.getLogger(IngestDocumentService.class);
    private final DownloadDocumentStatusProvider downloadLog;
    private final ContentSeparatorProvider separator;
    private final DuplicationProvider duplicationProvider;
    private final DocumentIngestedProvider notifier;

    public IngestDocumentService(
            DownloadDocumentStatusProvider downloadLog,
            ContentSeparatorProvider separator,
            DuplicationProvider duplicationProvider,
            DocumentIngestedProvider notifier) {
        this.downloadLog = downloadLog;
        this.separator = separator;
        this.duplicationProvider = duplicationProvider;
        this.notifier = notifier;
    }

    @Override
    public Map<String, Object> ingest(int bookId) {
        try {
            if (downloadLog.isDownloaded(bookId)) {
                return Map.of("book_id", bookId, "status", "already_downloaded");
            }

            log.info("Downloading book {}", bookId);
            FetchGutenbergBook fetcher = new FetchGutenbergBook();
            String content = fetcher.fetch(bookId);

            List<String> parts = separator.provide(content);
            String header = parts.get(0);
            String body = parts.get(1);

            duplicationProvider.duplicate(bookId, header, body);
            downloadLog.registerDownload(bookId);
            notifier.provide(bookId, "hazelcast://" + bookId);

            return Map.of("book_id", bookId, "status", "downloaded");
        } catch (Exception e) {
            log.error("Error ingesting book {}", bookId, e);
            return Map.of("book_id", bookId, "status", "error", "message", e.getMessage());
        }
    }
}

