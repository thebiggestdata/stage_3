package com.thebiggestdata.search;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.thebiggestdata.search.domain.service.SearchEngine;
import com.thebiggestdata.search.infrastructure.adapter.HazelcastInvertedIndexReaderAdapter;
import com.thebiggestdata.search.infrastructure.controller.SearchController;
import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        System.out.println("Starting Search Service...");

        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String membersEnv = System.getenv().getOrDefault("HZ_MEMBERS", "hazelcast1:5701");

        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);

        TcpIpConfig tcpIpConfig = networkConfig.getJoin().getTcpIpConfig();
        tcpIpConfig.setEnabled(true);

        for (String member : membersEnv.split(",")) {
            tcpIpConfig.addMember(member.trim());
        }

        MultiMapConfig multiMapConfig = new MultiMapConfig("inverted-index");
        multiMapConfig.setBackupCount(2);
        multiMapConfig.setStatisticsEnabled(true);
        config.addMultiMapConfig(multiMapConfig);

        System.out.println("Joining Hazelcast cluster...");
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        System.out.println("Search Service joined cluster: " + hazelcast.getCluster().getMembers());

        var indexReader = new HazelcastInvertedIndexReaderAdapter(hazelcast, "inverted-index");
        var searchEngine = new SearchEngine();
        var searchUseCase = new SearchBookUseCase(indexReader, searchEngine);

        Javalin app = Javalin.create().start(8080);
        var controller = new SearchController(searchUseCase);
        controller.registerRoutes(app);

        app.get("/health", ctx -> ctx.result("OK"));

        System.out.println("Search Service started on port 8080");
    }
}

