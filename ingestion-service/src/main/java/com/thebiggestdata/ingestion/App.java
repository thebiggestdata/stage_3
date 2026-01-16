package com.thebiggestdata.ingestion;

import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQIngestedBookProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastConfig;
import com.thebiggestdata.ingestion.model.BookContent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ingestion Service - Downloads books from Gutenberg and stores them in distributed Hazelcast datalake.
 * This service is a MEMBER of the Hazelcast cluster (not a client).
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        logger.info("=== Starting Ingestion Service ===");
        
        // Configuration from environment
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://localhost:61616");
        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String nodeId = System.getenv().getOrDefault("NODE_ID", "ingestion-1");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "7001"));
        
        logger.info("Configuration: cluster={}, nodeId={}, broker={}, port={}", 
                clusterName, nodeId, brokerUrl, port);

        // Initialize Hazelcast as MEMBER
        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcast = hazelcastConfig.initHazelcast(clusterName);
        
        // Get distributed maps
        IMap<Integer, BookContent> datalake = hazelcast.getMap("datalake");
        IMap<Integer, Boolean> downloadLog = hazelcast.getMap("download-log");
        
        logger.info("Connected to cluster. Datalake size: {}, Download log size: {}", 
                datalake.size(), downloadLog.size());

        // Initialize ActiveMQ notifier (can be disabled)
        ActiveMQIngestedBookProvider notifier = new ActiveMQIngestedBookProvider(brokerUrl);
        
        // Create Javalin REST API
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        // Health check endpoint
        app.get("/health", ctx -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("nodeId", nodeId);
            health.put("clusterSize", hazelcast.getCluster().getMembers().size());
            health.put("datalakeSize", datalake.size());
            ctx.json(health);
        });

        // Ingest single book
        app.post("/ingest/{book_id}", ctx -> {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            Map<String, Object> result = ingestBook(bookId, datalake, downloadLog, notifier, nodeId);
            ctx.json(result);
        });

        // Get ingestion status
        app.get("/ingest/status/{book_id}", ctx -> {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            Map<String, Object> result = new HashMap<>();
            result.put("book_id", bookId);
            result.put("downloaded", downloadLog.containsKey(bookId));
            result.put("in_datalake", datalake.containsKey(bookId));
            ctx.json(result);
        });

        // List all ingested books
        app.get("/ingest/list", ctx -> {
            Set<Integer> bookIds = datalake.keySet();
            Map<String, Object> result = new HashMap<>();
            result.put("count", bookIds.size());
            result.put("book_ids", bookIds);
            ctx.json(result);
        });

        // Batch ingest endpoint for benchmarking
        app.post("/ingest/batch", ctx -> {
            int startId = Integer.parseInt(ctx.queryParam("start") != null ? ctx.queryParam("start") : "1");
            int endId = Integer.parseInt(ctx.queryParam("end") != null ? ctx.queryParam("end") : "100");
            int threads = Integer.parseInt(ctx.queryParam("threads") != null ? ctx.queryParam("threads") : "5");
            
            // Run async
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);
            
            for (int id = startId; id <= endId; id++) {
                final int bookId = id;
                executor.submit(() -> {
                    try {
                        Map<String, Object> result = ingestBook(bookId, datalake, downloadLog, notifier, nodeId);
                        if ("success".equals(result.get("status")) || "already_exists".equals(result.get("status"))) {
                            success.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.MINUTES);
            
            Map<String, Object> result = new HashMap<>();
            result.put("requested", endId - startId + 1);
            result.put("success", success.get());
            result.put("failed", failed.get());
            result.put("datalake_size", datalake.size());
            ctx.json(result);
        });

        // Datalake stats
        app.get("/datalake/stats", ctx -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_books", datalake.size());
            stats.put("cluster_members", hazelcast.getCluster().getMembers().size());
            stats.put("local_entries", datalake.localKeySet().size());
            ctx.json(stats);
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Ingestion Service...");
            app.stop();
            hazelcast.shutdown();
            logger.info("Shutdown complete");
        }));

        logger.info("=== Ingestion Service started on port {} ===", port);
        logger.info("Cluster members: {}", hazelcast.getCluster().getMembers());
    }

    /**
     * Ingests a single book from Project Gutenberg into the distributed datalake.
     */
    private static Map<String, Object> ingestBook(int bookId, 
                                                   IMap<Integer, BookContent> datalake,
                                                   IMap<Integer, Boolean> downloadLog,
                                                   ActiveMQIngestedBookProvider notifier,
                                                   String nodeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("book_id", bookId);

        try {
            // Check if already ingested
            if (datalake.containsKey(bookId)) {
                result.put("status", "already_exists");
                result.put("message", "Book already in datalake");
                return result;
            }

            // Download from Gutenberg
            String content = downloadFromGutenberg(bookId);
            
            // Separate header and body
            String[] parts = separateContent(content);
            String header = parts[0];
            String body = parts[1];

            // Store in distributed datalake
            BookContent bookContent = new BookContent(bookId, header, body, nodeId);
            datalake.put(bookId, bookContent);
            downloadLog.put(bookId, true);

            // Notify indexing service via ActiveMQ
            notifier.provide(bookId, "hazelcast://datalake/" + bookId);

            result.put("status", "success");
            result.put("body_length", body.length());
            result.put("header_length", header.length());
            
            logger.info("Ingested book {} ({} chars)", bookId, body.length());

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            logger.error("Failed to ingest book {}: {}", bookId, e.getMessage());
        }

        return result;
    }

    /**
     * Downloads a book from Project Gutenberg.
     */
    private static String downloadFromGutenberg(int bookId) throws Exception {
        String url = "https://www.gutenberg.org/cache/epub/" + bookId + "/pg" + bookId + ".txt";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " for book " + bookId);
        }
        
        return response.body();
    }

    /**
     * Separates book content into header (metadata) and body.
     */
    private static String[] separateContent(String content) {
        String startMarker = "*** START OF";
        String endMarker = "*** END OF";
        
        int startIndex = content.indexOf(startMarker);
        if (startIndex == -1) {
            return new String[]{"", content};
        }
        
        int bodyStart = content.indexOf("\n", startIndex) + 1;
        int bodyEnd = content.indexOf(endMarker);
        
        if (bodyEnd == -1) {
            bodyEnd = content.length();
        }
        
        String header = content.substring(0, startIndex).trim();
        String body = content.substring(bodyStart, bodyEnd).trim();
        
        return new String[]{header, body};
    }
}