package com.thebiggestdata.ingestion.application.usecases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebiggestdata.ingestion.infrastructure.ports.old.BookProvider;
import com.thebiggestdata.ingestion.infrastructure.ports.old.BookStorage;
import com.thebiggestdata.ingestion.infrastructure.ports.old.Datalake;
import com.thebiggestdata.ingestion.infrastructure.ports.old.BookDownloadStatusStore;
import com.thebiggestdata.ingestion.infrastructure.ports.old.BookIngestedNotifier;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.IngestionResult;

import java.nio.file.Path;

public class IngestBook {

    private static final Logger log = LoggerFactory.getLogger(IngestBook.class);

    private final BookProvider bookProvider;
    private final BookStorage bookStorage;
    private final Datalake datalake;
    private final BookDownloadStatusStore downloadLog;
    private final BookIngestedNotifier notifier;

    public IngestBook(BookProvider bookProvider, BookStorage bookStorage, Datalake datalake,
                      BookDownloadStatusStore downloadLog, BookIngestedNotifier notifier) {
        this.bookProvider = bookProvider;
        this.bookStorage = bookStorage;
        this.datalake = datalake;
        this.downloadLog = downloadLog;
        this.notifier = notifier;
    }

    public IngestionResult execute(int bookId) {
        log.info("Start processing bookId={}", bookId);
        try {
            if (downloadLog.isDownloaded(bookId)) {
                log.warn("BookContent {} already downloaded, skipping.", bookId);
                return IngestionResult.alreadyIngested(bookId);
            }

            String[] rawContent = bookProvider.getBookContent(bookId);
            Path savedPath = bookStorage.saveBook(bookId, rawContent);

            BookContent bookContent = new BookContent(rawContent[0], rawContent[1]);
            datalake.save(bookId, bookContent);
            datalake.replicate(bookId);

            downloadLog.registerBookDownload(bookId);
            notifier.notifyIngestedBook(bookId);

            return IngestionResult.ingested(bookId, savedPath.toString());

        } catch (Exception e) {
            log.error("Error processing bookId {}: {}", bookId, e.getMessage());
            return IngestionResult.failed(bookId, e.getMessage());
        }
    }
}