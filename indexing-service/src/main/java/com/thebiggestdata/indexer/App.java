package com.thebiggestdata.indexer;

import com.thebiggestdata.ingestion.model.BookContent;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.javalin.Javalin;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Indexing Service - Reads books from Hazelcast datalake and builds inverted index.
 * This service is a MEMBER of the Hazelcast cluster.
 */
public class App {
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-záéíóúñü]+", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
            "be", "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare", "ought",
            "used", "it", "its", "this", "that", "these", "those", "i", "you", "he",
            "she", "we", "they", "what", "which", "who", "whom", "whose", "where",
            "when", "why", "how", "all", "each", "every", "both", "few", "more",
            "most", "other", "some", "such", "no", "nor", "not", "only", "own",
            "same", "so", "than", "too", "very", "just", "also", "now", "here"
    );

    public static void main(String[] args) {
        System.out.println("=== Starting Indexing Service ===");

        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://localhost:61616");
        String nodeId = System.getenv().getOrDefault("NODE_ID", "indexer-1");
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "ingested.document");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "7002"));
        int workerCount = Integer.parseInt(System.getenv().getOrDefault("WORKER_COUNT", "4"));

        System.out.printf("Config: cluster=%s, broker=%s, nodeId=%s, port=%d, workers=%d%n",
                clusterName, brokerUrl, nodeId, port, workerCount);

        // Initialize Hazelcast as MEMBER
        HazelcastInstance hazelcast = initHazelcast(clusterName);

        // Get distributed maps
        IMap<Integer, BookContent> datalake = hazelcast.getMap("datalake");
        IMap<String, Set<String>> invertedIndex = hazelcast.getMap("inverted-index");
        IMap<Integer, Boolean> processedDocs = hazelcast.getMap("processed-documents");
        IMap<Integer, Map<String, String>> bookMetadata = hazelcast.getMap("book-metadata");

        System.out.printf("Connected to cluster. Datalake: %d books, Index: %d terms%n",
                datalake.size(), invertedIndex.size());

        // Statistics
        AtomicInteger totalIndexed = new AtomicInteger(0);
        AtomicInteger totalTerms = new AtomicInteger(0);

        // Start ActiveMQ consumer workers
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        
        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    runIndexingWorker(workerId, brokerUrl, queueName, datalake, 
                            invertedIndex, processedDocs, bookMetadata, totalIndexed, totalTerms, nodeId);
                } catch (Exception e) {
                    System.err.printf("Worker %d failed: %s%n", workerId, e.getMessage());
                }
            });
        }

        // Create REST API
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        // Health endpoint
        app.get("/health", ctx -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("nodeId", nodeId);
            health.put("clusterSize", hazelcast.getCluster().getMembers().size());
            health.put("datalakeSize", datalake.size());
            health.put("indexTerms", invertedIndex.size());
            health.put("processedDocs", processedDocs.size());
            health.put("totalIndexed", totalIndexed.get());
            ctx.json(health);
        });

        // Manual index endpoint
        app.post("/index/{book_id}", ctx -> {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            Map<String, Object> result = indexBook(bookId, datalake, invertedIndex, 
                    processedDocs, bookMetadata, nodeId);
            if ("success".equals(result.get("status"))) {
                totalIndexed.incrementAndGet();
            }
            ctx.json(result);
        });

        // Index stats
        app.get("/index/stats", ctx -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_terms", invertedIndex.size());
            stats.put("processed_documents", processedDocs.size());
            stats.put("total_indexed_this_node", totalIndexed.get());
            stats.put("datalake_size", datalake.size());
            ctx.json(stats);
        });

        // Rebuild index
        app.post("/index/rebuild", ctx -> {
            processedDocs.clear();
            invertedIndex.clear();
            bookMetadata.clear();
            
            int count = 0;
            for (Integer bookId : datalake.keySet()) {
                Map<String, Object> result = indexBook(bookId, datalake, invertedIndex, 
                        processedDocs, bookMetadata, nodeId);
                if ("success".equals(result.get("status"))) {
                    count++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "completed");
            result.put("indexed", count);
            result.put("total_terms", invertedIndex.size());
            ctx.json(result);
        });

        // Search endpoint (basic)
        app.get("/search", ctx -> {
            String query = ctx.queryParam("q");
            if (query == null || query.isBlank()) {
                ctx.status(400).json(Map.of("error", "Missing query parameter 'q'"));
                return;
            }
            
            String[] terms = query.toLowerCase().split("\\s+");
            Set<String> results = null;
            
            for (String term : terms) {
                Set<String> docs = invertedIndex.get(term);
                if (docs == null) {
                    docs = Set.of();
                }
                if (results == null) {
                    results = new HashSet<>(docs);
                } else {
                    results.retainAll(docs);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("results", results != null ? results : Set.of());
            response.put("count", results != null ? results.size() : 0);
            ctx.json(response);
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Indexing Service...");
            executor.shutdownNow();
            app.stop();
            hazelcast.shutdown();
        }));

        System.out.printf("=== Indexing Service started on port %d ===%n", port);
    }

    /**
     * Creates a Hazelcast MEMBER instance.
     */
    private static HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig networkConfig = config.getNetworkConfig();
        int port = Integer.parseInt(System.getenv().getOrDefault("HZ_PORT", "5702"));
        networkConfig.setPort(port);
        networkConfig.setPortAutoIncrement(false);

        String publicAddress = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddress != null && !publicAddress.isBlank()) {
            networkConfig.setPublicAddress(publicAddress);
        }

        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAutoDetectionConfig().setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);

        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            for (String member : members.split(",")) {
                tcpIpConfig.addMember(member.trim());
            }
        }

        config.setProperty("hazelcast.wait.seconds.before.join", "0");
        
        config.addMapConfig(new MapConfig("datalake").setBackupCount(2));
        config.addMapConfig(new MapConfig("inverted-index").setBackupCount(2));
        config.addMapConfig(new MapConfig("processed-documents").setBackupCount(2));
        config.addMapConfig(new MapConfig("book-metadata").setBackupCount(2));

        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Worker that consumes messages from ActiveMQ and indexes books.
     */
    private static void runIndexingWorker(int workerId, String brokerUrl, String queueName,
                                          IMap<Integer, BookContent> datalake,
                                          IMap<String, Set<String>> invertedIndex,
                                          IMap<Integer, Boolean> processedDocs,
                                          IMap<Integer, Map<String, String>> bookMetadata,
                                          AtomicInteger totalIndexed,
                                          AtomicInteger totalTerms,
                                          String nodeId) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();
            
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Destination queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);

            System.out.printf("[Worker-%d] Listening on queue '%s'%n", workerId, queueName);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message message = consumer.receive(5000);
                    if (message == null) continue;

                    if (message instanceof TextMessage textMessage) {
                        String text = textMessage.getText();
                        String[] parts = text.split("\\|");
                        int bookId = Integer.parseInt(parts[0].trim());

                        Map<String, Object> result = indexBook(bookId, datalake, invertedIndex, 
                                processedDocs, bookMetadata, nodeId);
                        
                        if ("success".equals(result.get("status"))) {
                            totalIndexed.incrementAndGet();
                            System.out.printf("[Worker-%d] Indexed book %d (%d terms)%n", 
                                    workerId, bookId, result.get("terms"));
                        } else if ("already_processed".equals(result.get("status"))) {
                            System.out.printf("[Worker-%d] Book %d already processed%n", workerId, bookId);
                        }
                        
                        message.acknowledge();
                    }
                } catch (Exception e) {
                    System.err.printf("[Worker-%d] Error: %s%n", workerId, e.getMessage());
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            System.err.printf("[Worker-%d] Fatal error: %s%n", workerId, e.getMessage());
        }
    }

    /**
     * Indexes a single book into the inverted index.
     */
    private static Map<String, Object> indexBook(int bookId,
                                                  IMap<Integer, BookContent> datalake,
                                                  IMap<String, Set<String>> invertedIndex,
                                                  IMap<Integer, Boolean> processedDocs,
                                                  IMap<Integer, Map<String, String>> bookMetadata,
                                                  String nodeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("book_id", bookId);

        // Check if already processed (idempotent)
        if (processedDocs.containsKey(bookId)) {
            result.put("status", "already_processed");
            return result;
        }

        // Get book from datalake
        BookContent book = datalake.get(bookId);
        if (book == null) {
            result.put("status", "not_found");
            result.put("message", "Book not in datalake");
            return result;
        }

        try {
            // Tokenize and index
            String body = book.getBody();
            Map<String, Integer> termFrequencies = tokenize(body);

            // Build postings: term -> set of "bookId:frequency"
            String docId = String.valueOf(bookId);
            for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
                String term = entry.getKey();
                int frequency = entry.getValue();
                String posting = docId + ":" + frequency;

                invertedIndex.compute(term, (k, existingSet) -> {
                    Set<String> newSet = existingSet != null ? new HashSet<>(existingSet) : new HashSet<>();
                    newSet.add(posting);
                    return newSet;
                });
            }

            // Store metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("indexed_by", nodeId);
            metadata.put("indexed_at", String.valueOf(System.currentTimeMillis()));
            metadata.put("term_count", String.valueOf(termFrequencies.size()));
            bookMetadata.put(bookId, metadata);

            // Mark as processed
            processedDocs.put(bookId, true);

            result.put("status", "success");
            result.put("terms", termFrequencies.size());

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Tokenizes text and returns term frequencies.
     */
    private static Map<String, Integer> tokenize(String text) {
        Map<String, Integer> frequencies = new HashMap<>();
        
        var matcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= 2 && word.length() <= 30 && !STOP_WORDS.contains(word)) {
                frequencies.merge(word, 1, Integer::sum);
            }
        }
        
        return frequencies;
    }
}