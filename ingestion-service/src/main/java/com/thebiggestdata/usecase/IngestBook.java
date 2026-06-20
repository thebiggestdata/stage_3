package com.thebiggestdata.usecase;

import com.thebiggestdata.domain.gateway.*;
import com.thebiggestdata.domain.entity.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

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

    public Map<String, Object> execute(int bookId) {
        log.info("Start processing bookId={}", bookId);
        try {
            if (downloadLog.isDownloaded(bookId)) {
                log.warn("Book {} already downloaded, skipping.", bookId);
                return Map.of("status", "already_downloaded", "book_id", bookId);
            }

            String[] rawContent = bookProvider.getBookContent(bookId);
            Path savedPath = bookStorage.saveBook(bookId, rawContent);

            BookContent content = new BookContent(rawContent[0], rawContent[1]);
            datalake.save(bookId, content);
            datalake.replicate(bookId);

            downloadLog.registerBookDownload(bookId);
            notifier.notifyIngestedBook(bookId);

            return Map.of("status", "downloaded", "path", savedPath.toString());

        } catch (Exception e) {
            log.error("Error processing bookId {}: {}", bookId, e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}