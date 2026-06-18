package com.thebiggestdata.search;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.thebiggestdata.search.domain.service.SearchEngine;
import com.thebiggestdata.search.infrastructure.adapter.HazelcastInvertedIndexReaderAdapter;
import com.thebiggestdata.search.infrastructure.controller.SearchController;
import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        System.out.println("Starting Search Service...");

        HazelcastInstance hazelcast = createHazelcastClient();

        HazelcastInvertedIndexReaderAdapter indexReader = new HazelcastInvertedIndexReaderAdapter(hazelcast, "inverted-index");
        SearchEngine searchEngine = new SearchEngine();
        SearchBookUseCase searchUseCase = new SearchBookUseCase(indexReader, searchEngine);

        SearchController controller = new SearchController(searchUseCase);

        startWebServer(controller);
    }

    private static HazelcastInstance createHazelcastClient() {
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "search-cluster");
        String membersEnv = System.getenv().getOrDefault("HZ_MEMBERS", "hazelcast1,hazelcast2,hazelcast3");

        System.out.println("Connecting to Hazelcast Cluster: " + clusterName);

        ClientConfig config = new ClientConfig();
        config.setClusterName(clusterName);

        config.getNetworkConfig()
                .addAddress(membersEnv.split(","))
                .setSmartRouting(true)
                .setRedoOperation(true);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);

        System.out.println("Search Service connected. Members: " + client.getCluster().getMembers());
        return client;
    }

    private static void startWebServer(SearchController controller) {
        Javalin app = Javalin.create();
        controller.registerRoutes(app);
        app.get("/health", ctx -> ctx.result("OK"));
        app.start(8080);
        System.out.println("🌐 Search Service HTTP API running on port 8080");
    }
}