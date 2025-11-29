package com.thebiggestdata.ingestion.application.usecase;

import com.thebiggestdata.ingestion.domain.model.BookContent;
import com.thebiggestdata.ingestion.domain.model.BookParts;
import com.thebiggestdata.ingestion.domain.port.*;
import com.thebiggestdata.ingestion.domain.service.BookParser;
import com.thebiggestdata.ingestion.domain.service.BookPathGenerator;

import java.time.LocalDateTime;

public class IngestBookUseCase {

    private final DownloadBookPort downloadPort;
    private final DatalakeWritePort datalakePort;
    private final ReplicationPort replicationPort;
    private final EventPublisherPort eventPort;

    private final BookParser parser;
    private final BookPathGenerator pathGenerator;

    public IngestBookUseCase(
            DownloadBookPort downloadPort,
            DatalakeWritePort datalakePort,
            ReplicationPort replicationPort,
            EventPublisherPort eventPort,
            BookParser parser,
            BookPathGenerator pathGenerator
    ) {
        this.downloadPort = downloadPort;
        this.datalakePort = datalakePort;
        this.replicationPort = replicationPort;
        this.eventPort = eventPort;
        this.parser = parser;
        this.pathGenerator = pathGenerator;
    }

    public void ingest(int bookId) throws Exception {
        BookContent raw = downloadPort.download(bookId);
        BookParts parts = parser.parse(raw);

        LocalDateTime timestamp = LocalDateTime.now();

        pathGenerator.generate(parts, timestamp);
        datalakePort.write(parts, timestamp);
        replicationPort.replicate(parts);
        eventPort.publishIngestedEvent(parts);
    }
}
