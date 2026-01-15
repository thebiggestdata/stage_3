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
import java.util.concurrent.Executors;

public class App {

    public static void main(String[] args) {
        System.out.println("Starting Indexing Service (Client Mode)...");

        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String membersEnv = System.getenv().getOrDefault("HZ_MEMBERS", "hazelcast1:5701,hazelcast2:5701,hazelcast3:5701");
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "ingested.document");

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);
        clientConfig.getNetworkConfig().addAddress(membersEnv.split(","));

        clientConfig.getNetworkConfig().setSmartRouting(true);
        clientConfig.getNetworkConfig().setRedoOperation(true);

        System.out.println("Connecting to Hazelcast Cluster members: " + membersEnv);
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(clientConfig);

        System.out.println("Connected! Cluster size: " + hazelcast.getCluster().getMembers().size());

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
                hazelcast,
                eventConsumer,
                pathResolver,
                datalakeReader,
                tokenizerPipeline,
                postingBuilder,
                indexWriter
        );

        int workerCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Starting " + workerCount + " indexing workers listening on '" + queueName + "'...");

        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        useCase.run();
                    } catch (Exception e) {
                        if(!e.getMessage().contains("Interrupted")) {
                            System.err.println("Worker error: " + e.getMessage());
                        }
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    }
                }
            });
        }
    }
}