package org.example.control;

import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.example.shared.HazelcastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Control Module.
 *
 * Usage:  java -jar control-module.jar [port]
 * Default port: 7000
 */
public class ControlApp {

    private static final Logger log = LoggerFactory.getLogger(ControlApp.class);

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7000;

        log.info("Starting Control Module on port {}", port);

        HazelcastInstance hz = HazelcastFactory.createInstance();
        log.info("Hazelcast member started: {}", hz.getCluster().getLocalMember().getAddress());

        ControlController controller = new ControlController(hz);

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(port);

        controller.register(app);

        log.info("Control Module listening on http://localhost:{}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Control Module...");
            app.stop();
            hz.shutdown();
        }));
    }
}
