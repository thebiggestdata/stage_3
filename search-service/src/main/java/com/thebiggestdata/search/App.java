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

        // TODO use environment variables from the docker-compose
        ClientConfig config = new ClientConfig();
        config.setClusterName("biggestdata-cluster");

        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(config);

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
    }
}
