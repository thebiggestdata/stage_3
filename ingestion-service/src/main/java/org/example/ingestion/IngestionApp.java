package org.example.ingestion;

import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.example.shared.HazelcastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Ingestion Service.
 *
 * Usage:  java -jar ingestion-service.jar [port]
 * Default port: 7001
 */
public class IngestionApp {

    private static final Logger log = LoggerFactory.getLogger(IngestionApp.class);

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7001;

        log.info("Starting Ingestion Service on port {}", port);

        // Join (or form) the Hazelcast cluster
        HazelcastInstance hz = HazelcastFactory.createInstance();
        log.info("Hazelcast member started: {}", hz.getCluster().getLocalMember().getAddress());

        // Set up the REST API
        IngestionController controller = new IngestionController(hz);

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(port);

        controller.register(app);

        log.info("Ingestion Service listening on http://localhost:{}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Ingestion Service...");
            app.stop();
            hz.shutdown();
        }));
    }
}
