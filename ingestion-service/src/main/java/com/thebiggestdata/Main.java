package com.thebiggestdata;

import com.thebiggestdata.domain.gateway.*;
import com.thebiggestdata.usecase.BookIngestionScheduler;
import com.thebiggestdata.infrastructure.adapter.filesystem.BookArchiveByDate;
import com.thebiggestdata.infrastructure.adapter.cluster.HazelcastIngestionQueueStore;
import com.thebiggestdata.infrastructure.adapter.search.BookIngestionEndpoint;
import com.thebiggestdata.usecase.IngestBookUseCase;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQBookIngestedPublisher;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQIngestionSignalListener;
import com.thebiggestdata.infrastructure.adapter.bookprovider.*;
import com.thebiggestdata.infrastructure.adapter.cluster.HazelcastDatalake;
import com.thebiggestdata.infrastructure.adapter.cluster.ClusterManager;
import com.thebiggestdata.infrastructure.adapter.scheduler.IntervalScheduler;
import com.thebiggestdata.infrastructure.adapter.search.BookStatusReaderImpl;
import com.thebiggestdata.infrastructure.adapter.search.BookCatalogProviderImpl;
import com.thebiggestdata.usecase.IngestionPauseHandler;
import com.thebiggestdata.infrastructure.adapter.filesystem.DateTimePathBuilder;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String datalakePath = args[0];
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String clusterName = System.getenv().getOrDefault("HAZELCAST_CLUSTER_NAME", "SearchEngine");
        int replicationFactor = Integer.parseInt(System.getenv().getOrDefault("REPLICATION_FACTOR", "1"));
        int bufferFactor = Integer.parseInt(System.getenv().getOrDefault("INDEXING_BUFFER_FACTOR", "10"));

        PathBuilder pathGenerator = new DateTimePathBuilder(datalakePath);
        BookArchiveByDate storageDate = new BookArchiveByDate(pathGenerator);

        BookSource gutenbergProvider = new GutenbergBookSource(new GutenbergDownloader(), new GutenbergClient(),
                new GutenbergBookTextSplitter());

        ClusterManager hazelcastManager = new ClusterManager(clusterName, replicationFactor, gutenbergProvider,
                storageDate);

        Datalake datalake = new HazelcastDatalake(hazelcastManager.getHazelcastInstance(), hazelcastManager.getHazelcastReplicationExecuter());

        ActiveMQBookIngestedPublisher notifier = new ActiveMQBookIngestedPublisher(brokerUrl);
        BookDownloadStatusRepository statusStore = new BookDownloadJournal(hazelcastManager.getHazelcastInstance(), "log");

        IngestionPauseHandler pauseController = new IngestionPauseHandler();

        IngestBookUseCase ingestBookUseCase = new IngestBookUseCase(gutenbergProvider, storageDate, datalake, statusStore, notifier);

        IngestionQueueStore queueRepository = new HazelcastIngestionQueueStore(hazelcastManager.getHazelcastInstance());

        BookIngestionScheduler periodicLogic = new BookIngestionScheduler(ingestBookUseCase, pauseController,
                queueRepository, bufferFactor);

        BookCatalogProvider listBooksService = new BookCatalogProviderImpl(statusStore);
        BookStatusReader bookStatusService = new BookStatusReaderImpl(statusStore);

        BookIngestionEndpoint controller = new BookIngestionEndpoint(ingestBookUseCase, listBooksService,bookStatusService);

        ActiveMQIngestionSignalListener controlConsumer = new ActiveMQIngestionSignalListener(brokerUrl,
                "ingestion-control-consumer-" + java.util.UUID.randomUUID() , pauseController);

        IntervalScheduler scheduler = new IntervalScheduler();

        try {
            controlConsumer.start();
        } catch (Exception e) {
            log.error("Failed to start ActiveMQ consumer", e);
        }

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7001);

        app.post("/ingest/{book_id}", controller::ingestBook);
        app.get("/ingest/status/{book_id}", controller::status);
        app.get("/ingest/list", controller::listAllBooks);

        scheduler.schedule(periodicLogic::execute, 0, 100, TimeUnit.MILLISECONDS);
    }
}