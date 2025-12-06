package com.thebiggestdata.ingestion;


import com.thebiggestdata.ingestion.application.DocIngestionExecutor;
import com.thebiggestdata.ingestion.application.DocumentProviderController;
import com.thebiggestdata.ingestion.infrastructure.adapter.activeMQ.ActiveMQIngestedBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.DocumentStatusService;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.IngestDocumentService;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.ListDocumentService;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DateTimePathProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DocumentContentSeparator;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DownloadDocumentLog;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.StorageDocDate;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast.HazelcastDatalakeRecover;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast.HazelcastDuplicationManager;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ListDocumentsProvider;
import com.thebiggestdata.ingestion.infrastructure.port.PathProvider;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {

    public static void main(String[] args) throws Exception {
        String dataPath = (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty())
                ? args[0]
                : System.getenv().getOrDefault("DATALAKE_PATH", "datalake");
        String downloadLogPath = (args != null && args.length > 1 && args[1] != null && !args[1].isEmpty())
                ? args[1]
                : System.getenv().getOrDefault("DOWNLOAD_LOG_PATH", "downloads.log");
        System.out.println("Using datalake path: " + dataPath);
        System.out.println("Using download log path: " + downloadLogPath);
        String replicationEnv = System.getenv().getOrDefault("REPLICATION_FACTOR", System.getenv("FACTOR"));
        int replicationFactor = 3;
        if (replicationEnv != null && !replicationEnv.isEmpty()) {
            try {replicationFactor = Integer.parseInt(replicationEnv);}
            catch (NumberFormatException e) {
                System.out.println("Invalid replication factor='" + replicationEnv + "', using default=" + replicationFactor);
            }
        }
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        HazelcastDuplicationManager hazelcastManager = new HazelcastDuplicationManager("SearchEngine", replicationFactor);
        ActiveMQIngestedBookProvider notifier = new ActiveMQIngestedBookProvider(brokerUrl);
        PathProvider pathProvider = new DateTimePathProvider(dataPath);
        DocumentContentSeparator separator = new DocumentContentSeparator();
        StorageDocDate storageDate = new StorageDocDate(pathProvider, separator, hazelcastManager);
        DownloadDocumentLog bookDownloadLog = new DownloadDocumentLog(downloadLogPath);
        DownloadDocumentProvider ingestDocService = new IngestDocumentService(bookDownloadLog, storageDate, notifier);
        ListDocumentsProvider listDocsService = new ListDocumentService(bookDownloadLog);
        DocumentStatusProvider docStatusService = new DocumentStatusService(bookDownloadLog);
        DocumentProviderController controller = new DocumentProviderController(ingestDocService,
                listDocsService,
                docStatusService);
        HazelcastDatalakeRecover recovery = new HazelcastDatalakeRecover(hazelcastManager.getHazelcastInstance(),
                hazelcastManager.getNodeInfoProvider(),
                notifier);
        Path datalakeDir = Paths.get(dataPath);
        if (!Files.exists(datalakeDir)) {
            try {
                Files.createDirectories(datalakeDir);
                System.out.println("Created datalake path: " + datalakeDir.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[X] Unable to create datalake path '" + dataPath + "': " + e.getMessage());
                throw e;
            }
        }
        recovery.reloadMemoryFromDisk(dataPath);
        DocIngestionExecutor bookIngestionExecutor = new DocIngestionExecutor(hazelcastManager.getHazelcastInstance(),ingestDocService);
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7001);
        bookIngestionExecutor.setupBookQueue();
        bookIngestionExecutor.startPeriodicExecution();
        app.post("/ingest/{book_id}", controller::ingestDoc);
        app.get("/ingest/status/{book_id}", controller::status);
        app.get("/ingest/list", controller::listAllDocs);
    }
}
