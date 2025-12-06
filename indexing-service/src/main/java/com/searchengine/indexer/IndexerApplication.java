package com.searchengine.indexer;

import com.searchengine.indexer.config.HazelcastConfig;
import com.searchengine.indexer.service.BrokerConsumer;
import com.searchengine.indexer.service.DatalakeClient;
import com.searchengine.indexer.service.IndexingService;
import com.searchengine.indexer.service.TokenizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerApplication {

    private static final Logger logger = LoggerFactory.getLogger(IndexerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Indexer Service...");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "document.ingested");
        String hazelcastCluster = System.getenv().getOrDefault("HAZELCAST_CLUSTER", "search-cluster");
        String datalakePath = System.getenv().getOrDefault("DATALAKE_PATH", "datalake");

        HazelcastConfig hazelcastConfig = new HazelcastConfig(hazelcastCluster);
        TokenizerService tokenizerService = new TokenizerService();
        DatalakeClient datalakeClient = new DatalakeClient(datalakePath);
        IndexingService indexingService = new IndexingService(hazelcastConfig.getHazelcastInstance(), tokenizerService);
        BrokerConsumer brokerConsumer = new BrokerConsumer(brokerUrl, queueName);

        logger.info("Indexer started. Waiting for indexing messages...");

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
    }
}