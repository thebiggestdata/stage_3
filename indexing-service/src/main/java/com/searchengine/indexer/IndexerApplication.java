package com.searchengine.indexer;

import com.searchengine.indexer.config.HazelcastConfig;
import com.searchengine.indexer.controller.IndexingController;
import com.searchengine.indexer.service.BrokerConsumer;
import com.searchengine.indexer.service.DatalakeClient;
import com.searchengine.indexer.service.IndexingService;
import com.searchengine.indexer.service.TokenizerService;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerApplication {

    private static final Logger logger = LoggerFactory.getLogger(IndexerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Indexer Service...");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "ingested.documents");
        String hazelcastCluster = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String datalakePath = System.getenv().getOrDefault("DATALAKE_PATH", "datalake");

        HazelcastConfig hazelcastConfig = new HazelcastConfig(hazelcastCluster);
        TokenizerService tokenizerService = new TokenizerService();
        DatalakeClient datalakeClient = new DatalakeClient(datalakePath);
        IndexingService indexingService = new IndexingService(hazelcastConfig.getHazelcastInstance(), tokenizerService);
        IndexingController controller = new IndexingController(indexingService, datalakeClient, hazelcastConfig.getHazelcastInstance());

        // Start REST API
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/{book_id}", controller::indexBook);
        app.get("/index/status", controller::getStatus);
        app.get("/index/word/{word}", controller::getWordInfo);

        logger.info("REST API started on port 7002");

        // Start ActiveMQ consumer in a separate thread
        Thread consumerThread = new Thread(() -> {
            BrokerConsumer brokerConsumer = new BrokerConsumer(brokerUrl, queueName);
            logger.info("ActiveMQ consumer started. Waiting for indexing messages from queue: {}", queueName);

            while (true) {
                try {
                    var message = brokerConsumer.consumeMessage();
                    if (message != null) {
                        var document = datalakeClient.readDocument(message.bookId());
                        indexingService.indexDocument(document);
                        logger.info("[✓] Document {} indexed successfully.", message.bookId());
                    }
                } catch (Exception e) {
                    logger.error("[X] Error during indexing: {}", e.getMessage(), e);
                }
            }
        });
        consumerThread.setDaemon(false);
        consumerThread.start();

        logger.info("Indexer Service started successfully");
    }
}