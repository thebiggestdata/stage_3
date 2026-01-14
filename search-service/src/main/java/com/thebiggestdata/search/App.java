package com.thebiggestdata.search;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import com.thebiggestdata.search.application.usecase.SearchBookUseCase;
import com.thebiggestdata.search.infrastructure.adapter.HazelcastInvertedIndexReaderAdapter;
import com.thebiggestdata.search.infrastructure.controller.SearchController;

public class App {

    public static void main(String[] args) {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        HazelcastInvertedIndexReaderAdapter reader =
                new HazelcastInvertedIndexReaderAdapter(hazelcastInstance);

        SearchBookUseCase searchUseCase = new SearchBookUseCase(reader);

        Javalin app = Javalin.create().start(7000);

        new SearchController(app, searchUseCase);
    }
}