package com.thebiggestdata.ingestion;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.application.usecases.GetBookStatusUseCase;
import com.thebiggestdata.ingestion.application.usecases.InMemoryIngestionState;
import com.thebiggestdata.ingestion.application.usecases.IngestBookUseCase;
import com.thebiggestdata.ingestion.application.usecases.ListDownloadedBooksUseCase;
import com.thebiggestdata.ingestion.application.usecases.RunIngestionCycleUseCase;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.ActiveMQBookIngestedPublisher;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.ActiveMQIngestionControlConsumer;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.BookIngestedMessageMapper;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.IngestionControlMessageMapper;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.JmsMessageListener;
import com.thebiggestdata.ingestion.infrastructure.adapters.activemq.JmsMessageSender;
import com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider.GutenbergBookParser;
import com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider.GutenbergBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider.GutenbergHttpClient;
import com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider.GutenbergRetryPolicy;
import com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider.GutenbergUrlResolver;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.BookFileSerializer;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.BookFileWriter;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.BookPathResolver;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.FilesystemBookStorage;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastBookDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastBookIngestionGuard;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastBookReplicator;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastDatalake;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastDownloadedBooks;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastIndexedBooks;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastIngestionCapacity;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastPendingBooks;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastReplicationWorker;
import com.thebiggestdata.ingestion.infrastructure.adapters.scheduler.PeriodicScheduler;
import com.thebiggestdata.ingestion.infrastructure.adapters.web.BookProviderController;
import com.thebiggestdata.ingestion.infrastructure.config.HazelcastConfig;
import com.thebiggestdata.ingestion.infrastructure.config.IngestionConfiguration;
import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.ports.BookProvider;
import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.infrastructure.ports.DownloadedBooks;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;
import com.thebiggestdata.ingestion.model.BookNotFoundException;
import io.javalin.Javalin;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Main {

    private Main() {}

    public static void main(String[] arguments) {
        IngestionConfiguration configuration = IngestionConfiguration.load(arguments, System.getenv());
        Gson gson = new Gson();

        HazelcastInstance hazelcast = new HazelcastConfig().start(configuration.hazelcastClusterName());
        String nodeId = configuration.nodeId()
                .orElseGet(() -> hazelcast.getCluster().getLocalMember().getUuid().toString());

        BookProvider provider = new GutenbergBookProvider(
                new GutenbergUrlResolver(),
                new GutenbergHttpClient(configuration.providerTimeout()),
                new GutenbergRetryPolicy(
                        configuration.providerRetries(),
                        configuration.providerRetryDelay().toMillis()
                ),
                new GutenbergBookParser()
        );
        BookStorage storage = new FilesystemBookStorage(
                new BookPathResolver(configuration.datalakeRoot()),
                new BookFileSerializer(),
                new BookFileWriter()
        );

        HazelcastDatalake datalake = new HazelcastDatalake(hazelcast);
        BookDownloadStatus downloadStatus = new HazelcastBookDownloadStatus(hazelcast);
        DownloadedBooks downloadedBooks = new HazelcastDownloadedBooks(hazelcast);

        ConnectionFactory jmsFactory = new ActiveMQConnectionFactory(configuration.brokerUrl());
        JmsMessageSender eventSender = new JmsMessageSender(jmsFactory);
        JmsMessageListener controlListener = new JmsMessageListener(jmsFactory);
        IngestionState ingestionState = new InMemoryIngestionState();
        ActiveMQIngestionControlConsumer controlConsumer = new ActiveMQIngestionControlConsumer(
                controlListener,
                new IngestionControlMessageMapper(gson),
                ingestionState
        );

        IngestBookUseCase ingestBook = new IngestBookUseCase(
                downloadStatus,
                ingestionState,
                provider,
                new HazelcastBookIngestionGuard(hazelcast, nodeId, configuration.ingestionLease()),
                storage,
                datalake,
                new HazelcastBookReplicator(
                        hazelcast,
                        nodeId,
                        configuration.replicationFactor(),
                        configuration.replicationTimeout(),
                        configuration.replicationCheckInterval()
                ),
                new ActiveMQBookIngestedPublisher(
                        eventSender,
                        new BookIngestedMessageMapper(gson)
                )
        );

        RunIngestionCycleUseCase runIngestionCycle = new RunIngestionCycleUseCase(
                ingestionState,
                new HazelcastIngestionCapacity(hazelcast, configuration.indexingBufferFactor()),
                new HazelcastPendingBooks(
                        hazelcast,
                        configuration.pendingBookPollTimeout(),
                        configuration.pendingBookMaxAttempts()
                ),
                new HazelcastIndexedBooks(hazelcast),
                ingestBook
        );

        HazelcastReplicationWorker replicationWorker = new HazelcastReplicationWorker(
                hazelcast,
                nodeId,
                datalake,
                storage
        );
        PeriodicScheduler scheduler = new PeriodicScheduler(configuration.ingestionWorkers());

        BookProviderController controller = new BookProviderController(
                ingestBook,
                new ListDownloadedBooksUseCase(downloadedBooks),
                new GetBookStatusUseCase(downloadStatus),
                gson
        );
        Javalin application = httpApplication(controller, configuration.servicePort(), gson);

        replicationWorker.start();
        controlConsumer.start("ingestion-control-" + nodeId.replace(':', '-'));
        scheduler.schedule(
                runIngestionCycle::execute,
                0,
                configuration.schedulerInterval().toMillis(),
                TimeUnit.MILLISECONDS
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.close();
            replicationWorker.close();
            eventSender.close();
            controlListener.close();
            application.stop();
            hazelcast.shutdown();
        }, "ingestion-shutdown"));
    }

    private static Javalin httpApplication(BookProviderController controller, int port, Gson gson) {
        Javalin application = Javalin.create(config ->
                config.http.defaultContentType = "application/json"
        );
        application.exception(IllegalArgumentException.class, (exception, context) -> {
            context.status(400);
            context.result(gson.toJson(Map.of("error", exception.getMessage())));
        });
        application.exception(BookNotFoundException.class, (exception, context) -> {
            context.status(404);
            context.result(gson.toJson(Map.of(
                    "bookId", exception.bookId(),
                    "error", exception.getMessage()
            )));
        });
        application.post("/ingest/{book_id}", controller::ingestBook);
        application.get("/ingest/status/{book_id}", controller::status);
        application.get("/ingest/list", controller::listAllBooks);
        application.get("/health", context -> context.result("{\"status\":\"UP\"}"));
        return application.start(port);
    }
}
