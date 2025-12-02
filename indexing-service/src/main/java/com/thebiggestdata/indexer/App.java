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

public class App {

    public static void main(String[] args) {

        System.out.println("Starting Indexer...");

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("biggestdata-cluster");
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(clientConfig);

        String brokerUrl = "tcp://activemq:61616";
        String queueName = "document.ingested";

        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        var eventConsumer = new ActiveMQIngestedEventConsumerAdapter(factory, queueName);

        var pathResolver = new LocalDatalakePathResolver();
        var datalakeReader = new FileSystemDatalakeReaderAdapter();

        var normalizer = new TokenNormalizer();
        var tokenizer = new Tokenizer();
        var tokenizerPipeline = new TokenizerPipeline(normalizer, tokenizer);

        var postingBuilder = new PostingBuilder();

        var indexWriter = new HazelcastInvertedIndexWriterAdapter(hazelcast, "inverted-index");

        var useCase = new IndexBookUseCase(
                eventConsumer,
                pathResolver,
                datalakeReader,
                tokenizerPipeline,
                postingBuilder,
                indexWriter
        );

        System.out.println("Indexer started. Waiting for ingestion events...");

        while (true) {
            try {
                useCase.run();
                System.out.println("[✓] Document indexed successfully.");
            } catch (Exception e) {
                System.err.println("[X] Error during indexing: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
