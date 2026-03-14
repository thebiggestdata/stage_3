package org.example.indexing;

import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.example.shared.HazelcastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Indexing Service.
 *
 * Usage:  java -jar indexing-service.jar [port]
 * Default port: 7002
 */
public class IndexingApp {

    private static final Logger log = LoggerFactory.getLogger(IndexingApp.class);

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7002;

        log.info("Starting Indexing Service on port {}", port);

        HazelcastInstance hz = HazelcastFactory.createInstance();
        log.info("Hazelcast member started: {}", hz.getCluster().getLocalMember().getAddress());

        IndexingService indexingService = new IndexingService(hz);
        indexingService.start();   // subscribe to topic

        IndexingController controller = new IndexingController(indexingService);

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(port);

        controller.register(app);

        log.info("Indexing Service listening on http://localhost:{}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Indexing Service...");
            app.stop();
            hz.shutdown();
        }));
    }
}
