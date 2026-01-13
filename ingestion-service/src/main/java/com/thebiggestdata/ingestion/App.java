package com.thebiggestdata.ingestion;

import com.thebiggestdata.ingestion.application.DocIngestionExecutor;
import com.thebiggestdata.ingestion.application.DocumentProviderController;
import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQBookConsumer;
import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQIngestedBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.DocumentStatusService;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.IngestDocumentService;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.InitialBookLoader;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.ListDocumentService;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DateTimePathProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DocumentContentSeparator;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DownloadDocumentLog;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.StorageDocDate;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastConfig;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDatalakeListener;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDatalakeRecover;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDuplicationProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ListDocumentsProvider;
import com.thebiggestdata.ingestion.infrastructure.port.PathProvider;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        logger.info("=== Starting Ingestion Service ===");
        String datalakePath = System.getenv().getOrDefault("DATALAKE_PATH", "datalake");
        String downloadLogPath = System.getenv().getOrDefault("DOWNLOAD_LOG_PATH", "downloads.log");
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://localhost:61616");
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String nodeName = System.getenv().getOrDefault("NODE_NAME", "ingestion-1");
        int replicationFactor = getReplicationFactor();
        logger.info("Configuration:");
        logger.info("  Datalake path: {}", datalakePath);
        logger.info("  Download log: {}", downloadLogPath);
        logger.info("  Broker URL: {}", brokerUrl);
        logger.info("  Cluster name: {}", clusterName);
        logger.info("  Node name: {}", nodeName);
        logger.info("  Replication factor: {}", replicationFactor);
        ensureDirectoryExists(datalakePath);
        logger.info("Initializing Hazelcast member...");
        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcast = hazelcastConfig.initHazelcast(clusterName);
        NodeIdProvider nodeIdProvider = new NodeIdProvider(nodeName);
        logger.info("Node ID: {}", nodeIdProvider.nodeId());
        HazelcastDuplicationProvider duplicationProvider = new HazelcastDuplicationProvider(
                hazelcast,
                nodeIdProvider
        );
        HazelcastDatalakeListener datalakeListener = new HazelcastDatalakeListener(
                hazelcast,
                nodeIdProvider,
                replicationFactor
        );
        datalakeListener.registerListener();
        logger.info("Hazelcast datalake listener registered");
        PathProvider pathProvider = new DateTimePathProvider(datalakePath);
        DocumentContentSeparator separator = new DocumentContentSeparator();
        StorageDocDate storage = new StorageDocDate(pathProvider, separator, duplicationProvider);
        DownloadDocumentLog downloadLog = new DownloadDocumentLog(downloadLogPath);
        ActiveMQIngestedBookProvider notifier = new ActiveMQIngestedBookProvider(brokerUrl);
        DownloadDocumentProvider ingestService = new IngestDocumentService(
                downloadLog,
                storage,
                notifier
        );
        ListDocumentsProvider listService = new ListDocumentService(downloadLog);
        DocumentStatusProvider statusService = new DocumentStatusService(downloadLog);
        logger.info("Recovering existing data from disk...");
        HazelcastDatalakeRecover recovery = new HazelcastDatalakeRecover(
                hazelcast,
                nodeIdProvider,
                notifier
        );
        recovery.reloadMemoryFromDisk(datalakePath);
        logger.info("Data recovery completed");
        logger.info("Starting REST API on port 7001...");
        DocumentProviderController controller = new DocumentProviderController(
                ingestService,
                listService,
                statusService
        );
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7001);
        app.post("/ingest/{book_id}", controller::ingestDoc);
        app.get("/ingest/status/{book_id}", controller::status);
        app.get("/ingest/list", controller::listAllDocs);
        app.get("/health", ctx -> ctx.result("OK"));
        logger.info("REST API started successfully");
        DocIngestionExecutor executor = new DocIngestionExecutor(
                hazelcast,
                ingestService,
                nodeIdProvider
        );
        executor.setupBookQueue();
        executor.startPeriodicExecution();

        logger.info("Starting ActiveMQ consumer...");
        ActiveMQBookConsumer consumer = new ActiveMQBookConsumer(brokerUrl, ingestService);
        Thread consumerThread = new Thread(consumer, "ActiveMQ-Consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();
        boolean autoLoad = Boolean.parseBoolean(System.getenv().getOrDefault("AUTO_LOAD_BOOKS", "false"));
        if (autoLoad) {
            int startId = Integer.parseInt(System.getenv().getOrDefault("LOAD_START_ID", "1"));
            int endId = Integer.parseInt(System.getenv().getOrDefault("LOAD_END_ID", "10000"));
            int threads = Integer.parseInt(System.getenv().getOrDefault("LOAD_THREADS", "10"));

            logger.info("Auto-loading books enabled: {} to {}", startId, endId);

            Thread loaderThread = new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    InitialBookLoader loader = new InitialBookLoader(
                            ingestService,
                            nodeIdProvider,
                            startId,
                            endId,
                            threads
                    );
                    loader.loadBooks();
                } catch (Exception e) {
                    logger.error("Error in auto-loader", e);
                }
            }, "Book-Auto-Loader");
            loaderThread.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Ingestion Service...");
            consumer.stop();
            app.stop();
            hazelcast.shutdown();
            logger.info("Shutdown complete");
        }));
        logger.info("=== Ingestion Service started successfully ===");
        logger.info("Cluster members: {}", hazelcast.getCluster().getMembers());
    }

    private static int getReplicationFactor() {
        String replicationEnv = System.getenv().getOrDefault("REPLICATION_FACTOR", "3");
        try {
            return Integer.parseInt(replicationEnv);
        } catch (NumberFormatException e) {
            logger.warn("Invalid replication factor: {}, using default 3", replicationEnv);
            return 3;
        }
    }

    private static void ensureDirectoryExists(String path) throws IOException {
        Path dir = Paths.get(path);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            logger.info("Created directory: {}", dir.toAbsolutePath());
        }
    }
}