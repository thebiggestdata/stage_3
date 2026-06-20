package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.*;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;
import com.thebiggestdata.ingestion.model.IngestionResult;
import com.thebiggestdata.ingestion.model.ReplicationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public class IngestBookUseCase implements IngestBookPort {

    private static final Logger log = LoggerFactory.getLogger(IngestBookUseCase.class);

    private final BookProviderPort bookProvider;
    private final BookStoragePort bookStorage;
    private final DatalakePort datalake;
    private final BookDownloadStatusPort downloadStatus;
    private final BookIngestedNotifierPort notifier;
    private final int replicationFactor;

    public IngestBookUseCase(BookProviderPort bookProvider, BookStoragePort bookStorage, DatalakePort datalake,
                             BookDownloadStatusPort downloadStatus, BookIngestedNotifierPort notifier,
                             int replicationFactor) {

        this.bookProvider = bookProvider;
        this.bookStorage = bookStorage;
        this.datalake = datalake;
        this.downloadStatus = downloadStatus;
        this.notifier = notifier;
        this.replicationFactor = replicationFactor;

    }

    @Override
    public IngestionResult ingest(int bookId) {
        log.info("Start ingestion bookId={}", bookId);
        try {
            if (downloadStatus.isDownloaded(bookId)) {
                log.warn("Book {} already downloaded, skipping.", bookId);
                return IngestionResult.present(bookId);
            }

            BookContent content = bookProvider.getBookContent(bookId);
            Path savedPath = bookStorage.save(bookId, content);
            datalake.save(bookId, content);

            ReplicationResult replication = datalake.replicate(bookId, replicationFactor);
            if (!replication.isQuorumReached()) {
                log.error("Quorum not reached for book {}: {}", bookId, replication.failedPeers());
                return IngestionResult.failed(bookId, "Replication quorum not reached");
            }

            downloadStatus.markAsDownloaded(bookId);

            BookIngestedEvent bookIngestedEvent = new BookIngestedEvent(bookId);

            notifier.notify(bookIngestedEvent);

            return IngestionResult.ingested(bookId, savedPath.toString());

        } catch (Exception e) {
            log.error("Error ingesting bookId {}: {}", bookId, e.getMessage());
            return IngestionResult.failed(bookId, e.getMessage());
        }
    }
}
