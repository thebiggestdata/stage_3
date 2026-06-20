package com.thebiggestdata;

import com.google.gson.Gson;
import com.thebiggestdata.usecase.SearchExecutor;
import com.thebiggestdata.infrastructure.adapter.web.SearchEndpoint;
import com.thebiggestdata.usecase.FindBooksUseCase;
import com.thebiggestdata.infrastructure.adapter.sorter.RankByFrequency;
import com.thebiggestdata.infrastructure.adapter.sorter.RankById;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIndexRepository;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastMetadataRepository;
import com.thebiggestdata.infrastructure.config.ClusterConfig;
import com.thebiggestdata.domain.gateway.RankingStrategy;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) {
        ClusterConfig hzConfig = new ClusterConfig();

        HazelcastInstance hazelcastInstance = hzConfig.initHazelcast(System.getenv().getOrDefault("CLUSTER_NAME", "SearchEngine"));

        HazelcastIndexRepository indexStore = new HazelcastIndexRepository(hazelcastInstance);
        HazelcastMetadataRepository metadataStore = new HazelcastMetadataRepository(hazelcastInstance);

        Map<String, RankingStrategy> strategies = new HashMap<>();
        strategies.put("frequency", new RankByFrequency());
        strategies.put("id", new RankById());

        ExecutorService searchExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() - 3
        );

        SearchExecutor engine = new SearchExecutor(indexStore, searchExecutor);

        String sortingEnv = System.getenv("SORTING_CRITERIA");

        if (sortingEnv == null) sortingEnv = "frequency";

        RankingStrategy selectedStrategy = strategies.getOrDefault(
                sortingEnv.toLowerCase(),
                new RankByFrequency()
        );

        FindBooksUseCase search = new FindBooksUseCase(engine, metadataStore, selectedStrategy);

        SearchEndpoint controller = new SearchEndpoint(search);

        FindBooksUseCase searchService = new FindBooksUseCase(engine, metadataStore, selectedStrategy);

        SearchEndpoint searchController = new SearchEndpoint(searchService);

        Gson gson = new Gson();

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(7003);

        app.get("/search", searchController::search);
        app.get("/health", searchController::health);
    }
}