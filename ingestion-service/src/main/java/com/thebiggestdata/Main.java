package com.thebiggestdata;

import com.thebiggestdata.domain.gateway.*;
import com.thebiggestdata.usecase.BookIngestionPeriodicExecutor;
import com.thebiggestdata.infrastructure.adapter.filesystem.BookStorageDate;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIngestionRepository;
import com.thebiggestdata.infrastructure.adapter.web.BookProviderController;
import com.thebiggestdata.usecase.IngestBook;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQBookIngestedNotifier;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQIngestionControlConsumer;
import com.thebiggestdata.infrastructure.adapter.bookprovider.*;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastDatalake;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastManager;
import com.thebiggestdata.infrastructure.adapter.scheduler.PeriodicScheduler;
import com.thebiggestdata.infrastructure.adapter.web.BookStatusService;
import com.thebiggestdata.infrastructure.adapter.web.ListBooksService;
import com.thebiggestdata.usecase.IngestionPauseController;
import com.thebiggestdata.infrastructure.adapter.filesystem.DateTimePathGenerator;
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

        PathGenerator pathGenerator = new DateTimePathGenerator(datalakePath);
        BookStorageDate storageDate = new BookStorageDate(pathGenerator);

        BookProvider gutenbergProvider = new GutenbergBookProvider(new GutenbergFetch(), new GutenbergConnection(),
                new GutenbergBookContentSeparator());

        HazelcastManager hazelcastManager = new HazelcastManager(clusterName, replicationFactor, gutenbergProvider,
                storageDate);

        Datalake datalake = new HazelcastDatalake(hazelcastManager.getHazelcastInstance(), hazelcastManager.getHazelcastReplicationExecuter());

        ActiveMQBookIngestedNotifier notifier = new ActiveMQBookIngestedNotifier(brokerUrl);
        BookDownloadStatusStore statusStore = new BookDownloadLog(hazelcastManager.getHazelcastInstance(), "log");

        IngestionPauseController pauseController = new IngestionPauseController();

        IngestBook ingestBookUseCase = new IngestBook(gutenbergProvider, storageDate, datalake, statusStore, notifier);

        IngestionQueueRepository queueRepository = new HazelcastIngestionRepository(hazelcastManager.getHazelcastInstance());

        BookIngestionPeriodicExecutor periodicLogic = new BookIngestionPeriodicExecutor(ingestBookUseCase, pauseController,
                queueRepository, bufferFactor);

        BookListProvider listBooksService = new ListBooksService(statusStore);
        BookStatusProvider bookStatusService = new BookStatusService(statusStore);

        BookProviderController controller = new BookProviderController(ingestBookUseCase, listBooksService,bookStatusService);

        ActiveMQIngestionControlConsumer controlConsumer = new ActiveMQIngestionControlConsumer(brokerUrl,
                "ingestion-control-consumer-" + java.util.UUID.randomUUID() , pauseController);

        PeriodicScheduler scheduler = new PeriodicScheduler();

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