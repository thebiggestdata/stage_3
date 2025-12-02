package com.thebiggestdata.ingestion;

import com.thebiggestdata.ingestion.application.usecase.IngestBookUseCase;
import com.thebiggestdata.ingestion.domain.service.BookParser;
import com.thebiggestdata.ingestion.domain.service.BookPathGenerator;
import com.thebiggestdata.ingestion.infrastructure.adapter.*;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

public class App {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Usage: java App <bookId>");
            return;
        }

        int bookId = Integer.parseInt(args[0]);

        var downloadAdapter = new HttpBookFetcherAdapter();
        var datalakeAdapter = new FileSystemDatalakeAdapter();
        var replicationAdapter = new NoOpReplicationAdapter();

        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        String queueName = "document.ingested";

        var eventPublisher = new ActiveMQPublisherAdapter(factory, queueName);

        var parser = new BookParser();
        var pathGenerator = new BookPathGenerator();

        var useCase = new IngestBookUseCase(
                downloadAdapter,
                datalakeAdapter,
                replicationAdapter,
                eventPublisher,
                parser,
                pathGenerator
        );

        System.out.println("Starting ingestion for book " + bookId);
        useCase.ingest(bookId);
        System.out.println("Done.");
    }
}
