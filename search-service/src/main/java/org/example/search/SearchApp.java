package org.example.search;

import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.example.shared.HazelcastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Search Service.
 *
 * Usage:  java -jar search-service.jar [port]
 * Default port: 7003
 */
public class SearchApp {

    private static final Logger log = LoggerFactory.getLogger(SearchApp.class);

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7003;

        log.info("Starting Search Service on port {}", port);

        HazelcastInstance hz = HazelcastFactory.createInstance();
        log.info("Hazelcast member started: {}", hz.getCluster().getLocalMember().getAddress());

        SearchController controller = new SearchController(hz);

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(port);

        controller.register(app);

        log.info("Search Service listening on http://localhost:{}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Search Service...");
            app.stop();
            hz.shutdown();
        }));
    }
}
