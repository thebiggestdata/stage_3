package com.thebiggestdata.indexer;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
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
        System.out.println("Starting Indexing Service...");

        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String membersEnv = System.getenv().getOrDefault("HZ_MEMBERS", "hazelcast1:5701");

        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true);
        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        TcpIpConfig tcp = join.getTcpIpConfig();
        tcp.setEnabled(true);

        for (String member : membersEnv.split(",")) {
            tcp.addMember(member.trim());
            System.out.println("Added Hazelcast member: " + member.trim());
        }

        MultiMapConfig multiMapConfig = new MultiMapConfig("inverted-index");
        multiMapConfig.setBackupCount(2);
        config.addMultiMapConfig(multiMapConfig);

        MapConfig processedDocsConfig = new MapConfig("processed-documents");
        processedDocsConfig.setBackupCount(2);
        config.addMapConfig(processedDocsConfig);

        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        System.out.println("Indexing Service joined cluster: " + hazelcast.getCluster().getMembers());

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "indexing.documents");  // ✅ CORREGIR

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

        System.out.println("Indexing Service ready. Listening to queue: " + queueName);

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