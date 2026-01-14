package com.thebiggestdata.indexer;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
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

        Config config = new Config();
        config.setClusterName("search-cluster");
        config.getMultiMapConfig("inverted-index")
                .setBackupCount(2)
                .setAsyncBackupCount(1);

        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        System.out.println("Hazelcast member started.");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://localhost:61616");
        String queueName = "document.ingested";

        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        ActiveMQIngestedEventConsumerAdapter eventConsumer =
                new ActiveMQIngestedEventConsumerAdapter(factory, queueName);

        LocalDatalakePathResolver pathResolver = new LocalDatalakePathResolver();
        FileSystemDatalakeReaderAdapter datalakeReader = new FileSystemDatalakeReaderAdapter();

        TokenNormalizer normalizer = new TokenNormalizer();
        Tokenizer tokenizer = new Tokenizer();
        TokenizerPipeline tokenizerPipeline = new TokenizerPipeline(normalizer, tokenizer);

        PostingBuilder postingBuilder = new PostingBuilder();
        HazelcastInvertedIndexWriterAdapter indexWriter =
                new HazelcastInvertedIndexWriterAdapter(hazelcast, "inverted-index");

        IndexBookUseCase useCase = new IndexBookUseCase(
                hazelcast, eventConsumer, pathResolver, datalakeReader,
                tokenizerPipeline, postingBuilder, indexWriter
        );

        System.out.println("Indexer ready. Consuming from " + queueName);

        int workerCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        useCase.run();
                    } catch (Exception e) {
                        System.err.println("Worker error: " + e.getMessage());
                    }
                }
            });
        }

        Thread.currentThread().join();
    }
}
