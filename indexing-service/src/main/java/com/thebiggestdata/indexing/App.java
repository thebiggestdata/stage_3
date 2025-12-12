package com.thebiggestdata.indexing;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexing.application.IndexingController;
import com.thebiggestdata.indexing.infrastructure.adapter.activemq.ActiveMQIngestedDocumentConsumer;
import com.thebiggestdata.indexing.infrastructure.adapter.api.IndexBookService;
import com.thebiggestdata.indexing.infrastructure.adapter.api.IndexStatusService;
import com.thebiggestdata.indexing.infrastructure.adapter.api.WordQueryService;
import com.thebiggestdata.indexing.infrastructure.adapter.hazelcast.HazelcastConfig;
import com.thebiggestdata.indexing.infrastructure.adapter.hazelcast.HazelcastDatalakeReader;
import com.thebiggestdata.indexing.infrastructure.adapter.hazelcast.HazelcastInvertedIndex;
import com.thebiggestdata.indexing.infrastructure.adapter.tokenizer.SimpleTokenizer;
import com.thebiggestdata.indexing.infrastructure.port.DatalakeReader;
import com.thebiggestdata.indexing.infrastructure.port.IndexBookProvider;
import com.thebiggestdata.indexing.infrastructure.port.InvertedIndexWriter;
import com.thebiggestdata.indexing.infrastructure.port.Tokenizer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Starting Indexing Service ===");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "ingested.documents");
        String nodeName = System.getenv().getOrDefault("NODE_NAME", "indexing-node");

        logger.info("Configuration:");
        logger.info("  Broker URL: {}", brokerUrl);
        logger.info("  Cluster name: {}", clusterName);
        logger.info("  Queue name: {}", queueName);
        logger.info("  Node name: {}", nodeName);

        logger.info("Connecting to Hazelcast cluster...");
        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcast = hazelcastConfig.initHazelcast(clusterName);
        logger.info("Connected to Hazelcast cluster");
        logger.info("Cluster members: {}", hazelcast.getCluster().getMembers());

        DatalakeReader datalakeReader = new HazelcastDatalakeReader(hazelcast);
        InvertedIndexWriter indexWriter = new HazelcastInvertedIndex(hazelcast);
        Tokenizer tokenizer = new SimpleTokenizer();

        IndexBookProvider indexService = new IndexBookService(
                datalakeReader,
                tokenizer,
                indexWriter
        );

        IndexStatusService statusService = new IndexStatusService(hazelcast);
        WordQueryService wordQueryService = new WordQueryService(hazelcast);

        logger.info("Starting REST API on port 7002...");
        IndexingController controller = new IndexingController(
                indexService,
                statusService,
                wordQueryService
        );

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/{book_id}", controller::indexBook);
        app.get("/index/status", controller::status);
        app.get("/index/word/{word}", controller::queryWord);
        app.get("/health", ctx -> ctx.result("OK"));

        logger.info("REST API started successfully");

        logger.info("Starting ActiveMQ consumer...");
        ActiveMQIngestedDocumentConsumer consumer = new ActiveMQIngestedDocumentConsumer(
                brokerUrl,
                queueName,
                indexService
        );
        Thread consumerThread = new Thread(consumer, "ActiveMQ-Consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();
        logger.info("ActiveMQ consumer started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Indexing Service...");
            consumer.stop();
            app.stop();
            hazelcast.shutdown();
            logger.info("Shutdown complete");
        }));

        logger.info("=== Indexing Service started successfully ===");
    }
}