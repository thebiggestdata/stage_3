package com.thebiggestdata.indexer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexer.application.usecase.IndexBookUseCase;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import com.thebiggestdata.indexer.domain.service.TokenNormalizer;
import com.thebiggestdata.indexer.domain.service.Tokenizer;
import com.thebiggestdata.indexer.domain.service.TokenizerPipeline;
import com.thebiggestdata.indexer.infrastructure.adapter.ActiveMQIngestedEventConsumerAdapter;
import com.thebiggestdata.indexer.infrastructure.adapter.FileSystemDatalakeReaderAdapter;
import com.thebiggestdata.indexer.infrastructure.adapter.HazelcastInvertedIndexWriterAdapter;
import com.thebiggestdata.indexer.infrastructure.adapter.LocalDatalakePathResolver;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.concurrent.ExecutorService;

public class App {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Starting Indexer...");
        ClientConfig config = new ClientConfig();
        config.setClusterName("search-cluster");

        config.getNetworkConfig().setSmartRouting(false);

        config.getNetworkConfig().addAddress(
                "hazelcast1",
                "hazelcast2",
                "hazelcast3"
        );

        config.getConnectionStrategyConfig().getConnectionRetryConfig()
                .setClusterConnectTimeoutMillis(Long.MAX_VALUE);

        System.out.println("Connecting to Hazelcast Cluster...");
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(config);
        System.out.println("Connected.");


        String brokerUrl = "tcp://activemq:61616";
        String queueName = "document.ingested";

        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        ActiveMQIngestedEventConsumerAdapter eventConsumer = new ActiveMQIngestedEventConsumerAdapter(factory, queueName);

        LocalDatalakePathResolver pathResolver = new LocalDatalakePathResolver();
        FileSystemDatalakeReaderAdapter datalakeReader = new FileSystemDatalakeReaderAdapter();

        TokenNormalizer normalizer = new TokenNormalizer();
        Tokenizer tokenizer = new Tokenizer();
        TokenizerPipeline tokenizerPipeline = new TokenizerPipeline(normalizer, tokenizer);

        PostingBuilder postingBuilder = new PostingBuilder();

        HazelcastInvertedIndexWriterAdapter indexWriter = new HazelcastInvertedIndexWriterAdapter(hazelcast, "inverted-index");

        IndexBookUseCase useCase = new IndexBookUseCase(
                eventConsumer,
                pathResolver,
                datalakeReader,
                tokenizerPipeline,
                postingBuilder,
                indexWriter
        );

        System.out.println("Indexer started. Waiting for ingestion events...");

        int workerCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        useCase.run();
                        System.out.println("[Thread " + Thread.currentThread().getId() + "] Document indexed.");
                    } catch (Exception e) {
                        System.err.println("Worker error: " + e.getMessage());
                    }
                }
            });
        }
        Thread.currentThread().join();
    }
}
