package com.thebiggestdata.search;

import com.hazelcast.config.Config;
import com.hazelcast.config.MultiMapConfig;
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
        Config config = new Config();
        config.setClusterName("search-cluster");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("hazelcast1:5701")
                .addMember("hazelcast2:5701")
                .addMember("hazelcast3:5701");
        MultiMapConfig multiMapConfig = new MultiMapConfig("inverted-index");
        multiMapConfig.setBackupCount(2);
        multiMapConfig.setStatisticsEnabled(false);
        config.addMultiMapConfig(multiMapConfig);
        System.out.println("Joining Hazelcast cluster...");
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        System.out.println("Search Service joined cluster as MEMBER");
        var indexReader = new HazelcastInvertedIndexReaderAdapter(
                hazelcast,
                "inverted-index"
        );
        var searchEngine = new SearchEngine();
        var searchUseCase = new SearchBookUseCase(indexReader, searchEngine);
        Javalin app = Javalin.create().start(8080);
        var controller = new SearchController(searchUseCase);
        controller.registerRoutes(app);
        System.out.println("Search Service started on port 8080");
        System.out.println("Ready to serve queries from distributed index");
    }
}

