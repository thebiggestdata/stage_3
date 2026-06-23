package com.thebiggestdata.search;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.application.usecases.CheckHealthUseCase;
import com.thebiggestdata.search.application.usecases.ContentSearchEngine;
import com.thebiggestdata.search.application.usecases.FindBooksUseCase;
import com.thebiggestdata.search.application.usecases.SearchResultAssembler;
import com.thebiggestdata.search.infrastructure.adapters.hazelcast.HazelcastHealthProbe;
import com.thebiggestdata.search.infrastructure.adapters.hazelcast.HazelcastIndexGenerationStore;
import com.thebiggestdata.search.infrastructure.adapters.hazelcast.HazelcastIndexStore;
import com.thebiggestdata.search.infrastructure.adapters.hazelcast.HazelcastMetadataStore;
import com.thebiggestdata.search.infrastructure.adapters.sorter.SortByFrequency;
import com.thebiggestdata.search.infrastructure.adapters.sorter.SortById;
import com.thebiggestdata.search.infrastructure.adapters.tokenizer.SimpleQueryTokenizer;
import com.thebiggestdata.search.infrastructure.adapters.web.SearchController;
import com.thebiggestdata.search.infrastructure.adapters.web.SearchRequestMapper;
import com.thebiggestdata.search.infrastructure.adapters.web.SearchResponsePresenter;
import com.thebiggestdata.search.infrastructure.config.HazelcastConfig;
import com.thebiggestdata.search.infrastructure.config.SearchConfiguration;
import com.thebiggestdata.search.infrastructure.ports.SortingStrategy;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {

    private Main() {}

    public static void main(String[] arguments) {
        SearchConfiguration configuration = SearchConfiguration.load(System.getenv());
        HazelcastInstance hazelcast = new HazelcastConfig().start(configuration.hazelcastClusterName());
        HazelcastIndexGenerationStore generations = new HazelcastIndexGenerationStore(hazelcast);
        ExecutorService searchExecutor = Executors.newFixedThreadPool(configuration.searchThreads());

        SortingStrategy sorting = configuration.sortOrder() == SearchConfiguration.SortOrder.ID
                ? new SortById()
                : new SortByFrequency();
        FindBooksUseCase findBooks = new FindBooksUseCase(
                generations,
                new ContentSearchEngine(
                        new HazelcastIndexStore(hazelcast),
                        new SimpleQueryTokenizer(),
                        searchExecutor
                ),
                new HazelcastMetadataStore(hazelcast),
                new SearchResultAssembler(),
                sorting
        );
        SearchController controller = new SearchController(
                findBooks,
                new CheckHealthUseCase(generations, new HazelcastHealthProbe(hazelcast)),
                new SearchRequestMapper(),
                new SearchResponsePresenter()
        );

        Javalin application = httpApplication(controller, configuration.servicePort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            application.stop();
            searchExecutor.shutdownNow();
            hazelcast.shutdown();
        }, "search-shutdown"));
    }

    private static Javalin httpApplication(SearchController controller, int port) {
        Gson gson = new Gson();
        Javalin application = Javalin.create(config -> config.jsonMapper(new JsonMapper() {
            @Override
            public String toJsonString(Object object, Type type) {
                return gson.toJson(object, type);
            }

            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                return gson.fromJson(json, targetType);
            }
        }));
        application.exception(IllegalArgumentException.class, (exception, context) ->
                context.status(400).json(new SearchResponsePresenter().formatError(exception.getMessage()))
        );
        application.get("/search", controller::search);
        application.get("/health", controller::health);
        return application.start(port);
    }
}
