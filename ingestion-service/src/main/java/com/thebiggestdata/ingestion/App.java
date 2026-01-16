package com.thebiggestdata.ingestion;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.application.DocIngestionExecutor;
import com.thebiggestdata.ingestion.application.DocumentProviderController;
import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQIngestedBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.*;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DocumentContentSeparator;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.InMemoryDownloadStatus;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastConfig;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDatalakeListener;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDuplicationProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ListDocumentsProvider;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Starting Ingestion Service (RAM ONLY) ===");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String nodeName = System.getenv().getOrDefault("NODE_NAME", "ingestion-1");
        int replicationFactor = 3;

        // 1. Infraestructura
        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcast = hazelcastConfig.initHazelcast(clusterName);
        NodeIdProvider nodeIdProvider = new NodeIdProvider(nodeName);

        // 2. Adaptadores Hazelcast
        HazelcastDuplicationProvider duplicationProvider = new HazelcastDuplicationProvider(hazelcast, nodeIdProvider);
        HazelcastDatalakeListener datalakeListener = new HazelcastDatalakeListener(hazelcast, nodeIdProvider, replicationFactor);
        hazelcast.getMap("datalake").addEntryListener(datalakeListener, true);

        // 3. Componentes de Negocio
        DocumentContentSeparator separator = new DocumentContentSeparator();
        DownloadDocumentStatusProvider statusProvider = new InMemoryDownloadStatus(hazelcast);
        ActiveMQIngestedBookProvider notifier = new ActiveMQIngestedBookProvider(brokerUrl);

        // 4. Servicios Principales
        DownloadDocumentProvider ingestService = new IngestDocumentService(
                statusProvider,
                separator,
                duplicationProvider,
                notifier
        );
        ListDocumentsProvider listService = new ListDocumentService(statusProvider);
        DocumentStatusProvider statusService = new DocumentStatusService(statusProvider);

        // 5. REST API
        DocumentProviderController controller = new DocumentProviderController(ingestService, listService, statusService);
        Javalin app = Javalin.create().start(7001);
        app.post("/ingest/{book_id}", controller::ingestDoc);
        app.get("/ingest/status/{book_id}", controller::status);
        app.get("/ingest/list", controller::listAllDocs);

        // 6. Ejecución Automática
        DocIngestionExecutor executor = new DocIngestionExecutor(hazelcast, ingestService, nodeIdProvider);
        if (Boolean.parseBoolean(System.getenv().getOrDefault("AUTO_LOAD_BOOKS", "true"))) {
            executor.setupBookQueue();
            executor.startPeriodicExecution();
        }
    }
}
