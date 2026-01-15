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
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String membersEnv = System.getenv().getOrDefault("HZ_MEMBERS", "localhost:5701");

        System.out.println("Connecting to Hazelcast Cluster: " + clusterName);
        System.out.println("Members: " + membersEnv);

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
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

        Javalin app = Javalin.create();
        controller.registerRoutes(app);
        app.get("/health", ctx -> ctx.result("OK"));
        app.start(port);

        System.out.println("🌐 Search Service HTTP API running on port " + port);
    }
}