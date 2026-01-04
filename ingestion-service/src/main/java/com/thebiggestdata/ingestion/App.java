package com.thebiggestdata.ingestion;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.domain.service.CrawlerService;
import com.thebiggestdata.ingestion.application.usecase.IngestBookUseCase;
import com.thebiggestdata.ingestion.domain.service.BookParser;
import com.thebiggestdata.ingestion.domain.service.BookPathGenerator;
import com.thebiggestdata.ingestion.infrastructure.adapter.*;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

public class App {

    public static void main(String[] args) throws Exception {

        System.out.println("Starting Crawler...");

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

        HazelcastBookIdProviderAdapter idProvider = new HazelcastBookIdProviderAdapter(hazelcast);

        HttpBookFetcherAdapter downloadAdapter = new HttpBookFetcherAdapter();
        FileSystemDatalakeAdapter datalakeAdapter = new FileSystemDatalakeAdapter();
        NoOpReplicationAdapter replicationAdapter = new NoOpReplicationAdapter();

        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://activemq:61616");
        ActiveMQPublisherAdapter eventPublisher = new ActiveMQPublisherAdapter(factory, "document.ingested");

        BookParser parser = new BookParser();
        BookPathGenerator pathGenerator = new BookPathGenerator();

        IngestBookUseCase useCase = new IngestBookUseCase(
                downloadAdapter,
                datalakeAdapter,
                replicationAdapter,
                eventPublisher,
                parser,
                pathGenerator
        );
        CrawlerService crawlerService = new CrawlerService(useCase, idProvider);

        crawlerService.crawl();
    }
}