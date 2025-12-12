package com.thebiggestdata.search;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.application.SearchController;
import com.thebiggestdata.search.infrastructure.adapter.api.BookCheckService;
import com.thebiggestdata.search.infrastructure.adapter.api.SearchQueryService;
import com.thebiggestdata.search.infrastructure.adapter.api.SearchStatsService;
import com.thebiggestdata.search.infrastructure.adapter.hazelcast.HazelcastConfig;
import com.thebiggestdata.search.infrastructure.adapter.hazelcast.HazelcastInvertedIndexReader;
import com.thebiggestdata.search.infrastructure.port.BookCheckProvider;
import com.thebiggestdata.search.infrastructure.port.InvertedIndexReader;
import com.thebiggestdata.search.infrastructure.port.SearchQueryProvider;
import com.thebiggestdata.search.infrastructure.port.SearchStatsProvider;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Starting Search Service ===");
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String nodeName = System.getenv().getOrDefault("NODE_NAME", "search-node");
        logger.info("Configuration:");
        logger.info("  Cluster name: {}", clusterName);
        logger.info("  Node name: {}", nodeName);
        logger.info("Connecting to Hazelcast cluster...");
        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcast = hazelcastConfig.initHazelcast(clusterName);
        logger.info("Connected to Hazelcast cluster");
        logger.info("Cluster members: {}", hazelcast.getCluster().getMembers());
        InvertedIndexReader indexReader = new HazelcastInvertedIndexReader(hazelcast);
        SearchQueryProvider searchService = new SearchQueryService(indexReader);
        BookCheckProvider bookCheckService = new BookCheckService(hazelcast);
        SearchStatsProvider statsService = new SearchStatsService(hazelcast);
        logger.info("Starting REST API on port 7003...");
        SearchController controller = new SearchController(
                searchService,
                bookCheckService,
                statsService
        );

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7003);

        app.get("/search", controller::search);
        app.get("/search/stats", controller::stats);
        app.get("/search/{book_id}", controller::checkBook);
        app.get("/health", ctx -> ctx.result("OK"));
        logger.info("REST API started successfully");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Search Service...");
            app.stop();
            hazelcast.shutdown();
            logger.info("Shutdown complete");
        }));
        logger.info("=== Search Service started successfully ===");
    }
}