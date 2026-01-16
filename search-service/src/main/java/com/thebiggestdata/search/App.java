package com.thebiggestdata.search;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.javalin.Javalin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Search Service - Queries the distributed inverted index.
 * This service is a MEMBER of the Hazelcast cluster.
 */
public class App {

    public static void main(String[] args) {
        System.out.println("=== Starting Search Service ===");

        String clusterName = System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine");
        String nodeId = System.getenv().getOrDefault("NODE_ID", "search-1");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "7003"));
        String sortBy = System.getenv().getOrDefault("SORT_BY", "frequency"); // frequency or id

        System.out.printf("Config: cluster=%s, nodeId=%s, port=%d, sortBy=%s%n",
                clusterName, nodeId, port, sortBy);

        // Initialize Hazelcast as MEMBER
        HazelcastInstance hazelcast = initHazelcast(clusterName);

        // Get distributed maps
        IMap<String, Set<String>> invertedIndex = hazelcast.getMap("inverted-index");
        IMap<Integer, Map<String, String>> bookMetadata = hazelcast.getMap("book-metadata");
        IMap<Integer, Boolean> processedDocs = hazelcast.getMap("processed-documents");

        System.out.printf("Connected to cluster. Index: %d terms, Processed: %d docs%n",
                invertedIndex.size(), processedDocs.size());

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
            health.put("indexTerms", invertedIndex.size());
            health.put("indexedBooks", processedDocs.size());
            ctx.json(health);
        });

        // Search endpoint
        app.get("/search", ctx -> {
            String query = ctx.queryParam("q");
            String mode = ctx.queryParam("mode") != null ? ctx.queryParam("mode") : "and";
            int limit = ctx.queryParam("limit") != null ? Integer.parseInt(ctx.queryParam("limit")) : 100;

            if (query == null || query.isBlank()) {
                ctx.status(400).json(Map.of("error", "Missing query parameter 'q'"));
                return;
            }

            long startTime = System.currentTimeMillis();
            
            // Parse query terms
            String[] terms = query.toLowerCase().split("\\s+");
            
            // Search for each term
            Map<Integer, Double> scores = new HashMap<>();
            
            for (String term : terms) {
                Set<String> postings = invertedIndex.get(term);
                if (postings == null || postings.isEmpty()) {
                    if ("and".equals(mode)) {
                        // If AND mode and term not found, no results
                        scores.clear();
                        break;
                    }
                    continue;
                }

                for (String posting : postings) {
                    // posting format: "bookId:frequency"
                    String[] parts = posting.split(":");
                    int bookId = Integer.parseInt(parts[0]);
                    double frequency = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;

                    if ("and".equals(mode)) {
                        // For AND, only keep if already exists or first term
                        if (scores.isEmpty() || scores.containsKey(bookId)) {
                            scores.merge(bookId, frequency, Double::sum);
                        }
                    } else {
                        // For OR, add all
                        scores.merge(bookId, frequency, Double::sum);
                    }
                }

                // For AND mode after first term, keep only intersection
                if ("and".equals(mode) && !scores.isEmpty()) {
                    Set<String> nextPostings = invertedIndex.get(term);
                    if (nextPostings != null) {
                        Set<Integer> nextBookIds = nextPostings.stream()
                                .map(p -> Integer.parseInt(p.split(":")[0]))
                                .collect(Collectors.toSet());
                        scores.keySet().retainAll(nextBookIds);
                    }
                }
            }

            // Sort results
            List<Map<String, Object>> results;
            if ("frequency".equals(sortBy)) {
                results = scores.entrySet().stream()
                        .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                        .limit(limit)
                        .map(e -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("book_id", e.getKey());
                            item.put("score", e.getValue());
                            // Add metadata if available
                            Map<String, String> meta = bookMetadata.get(e.getKey());
                            if (meta != null) {
                                item.put("metadata", meta);
                            }
                            return item;
                        })
                        .collect(Collectors.toList());
            } else {
                results = scores.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .limit(limit)
                        .map(e -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("book_id", e.getKey());
                            item.put("score", e.getValue());
                            return item;
                        })
                        .collect(Collectors.toList());
            }

            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("mode", mode);
            response.put("terms", terms);
            response.put("total_results", results.size());
            response.put("results", results);
            response.put("search_time_ms", duration);
            
            ctx.json(response);
        });

        // Index stats
        app.get("/stats", ctx -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_terms", invertedIndex.size());
            stats.put("indexed_books", processedDocs.size());
            stats.put("cluster_members", hazelcast.getCluster().getMembers().size());
            stats.put("local_index_entries", invertedIndex.localKeySet().size());
            ctx.json(stats);
        });

        // Get specific term postings (for debugging)
        app.get("/term/{term}", ctx -> {
            String term = ctx.pathParam("term").toLowerCase();
            Set<String> postings = invertedIndex.get(term);
            
            Map<String, Object> response = new HashMap<>();
            response.put("term", term);
            response.put("found", postings != null);
            response.put("postings", postings != null ? postings : Set.of());
            response.put("document_count", postings != null ? postings.size() : 0);
            ctx.json(response);
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Search Service...");
            app.stop();
            hazelcast.shutdown();
        }));

        System.out.printf("=== Search Service started on port %d ===%n", port);
    }

    /**
     * Creates a Hazelcast MEMBER instance.
     */
    private static HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig networkConfig = config.getNetworkConfig();
        int port = Integer.parseInt(System.getenv().getOrDefault("HZ_PORT", "5703"));
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
        
        config.addMapConfig(new MapConfig("inverted-index").setBackupCount(2));
        config.addMapConfig(new MapConfig("processed-documents").setBackupCount(2));
        config.addMapConfig(new MapConfig("book-metadata").setBackupCount(2));

        return Hazelcast.newHazelcastInstance(config);
    }
}