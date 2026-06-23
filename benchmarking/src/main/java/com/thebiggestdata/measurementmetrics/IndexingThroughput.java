package com.thebiggestdata.measurementmetrics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.ArrayList;
import java.util.List;

public final class IndexingThroughput {

    private static final String INDEX_GENERATIONS = "index-generations";
    private static final String TOKEN_COUNTS = "book-token-counts";

    private IndexingThroughput() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(clientConfig());

        try {
            List<Double> rates = new ArrayList<>();
            int warmupIterations = 5;
            int measurementIterations = 10;

            for (int iteration = 0; iteration < warmupIterations + measurementIterations; iteration++) {
                String generation = activeGeneration(hazelcast);
                long startTokens = totalTokens(hazelcast, generation);
                long startTime = System.nanoTime();
                Thread.sleep(10_000);

                if (!generation.equals(activeGeneration(hazelcast))) {
                    throw new IllegalStateException("Index generation changed during the benchmark");
                }
                long endTokens = totalTokens(hazelcast, generation);
                double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                double tokensPerSecond = (endTokens - startTokens) / seconds;

                boolean warmup = iteration < warmupIterations;
                int phaseIteration = warmup ? iteration + 1 : iteration - warmupIterations + 1;
                if (!warmup) {
                    rates.add(tokensPerSecond);
                }
                System.out.printf("%s %2d: %.1f tokens/s (%.1fs) [generation=%s, tokens=%d]%n",
                        warmup ? "warmup" : "iter",
                        phaseIteration,
                        tokensPerSecond,
                        seconds,
                        generation,
                        endTokens - startTokens);
            }

            IngestionRate.printResults("IndexingThroughput", "tokens/s", rates);
        } finally {
            hazelcast.shutdown();
        }
    }

    private static String activeGeneration(HazelcastInstance hazelcast) {
        IMap<String, String> generations = hazelcast.getMap(INDEX_GENERATIONS);
        return generations.getOrDefault("active", "initial");
    }

    private static long totalTokens(HazelcastInstance hazelcast, String generation) {
        IMap<Integer, Integer> tokenCounts = hazelcast.getMap(TOKEN_COUNTS + ":" + generation);
        return tokenCounts.values().stream().mapToLong(Integer::longValue).sum();
    }

    private static ClientConfig clientConfig() {
        ClientConfig config = new ClientConfig();
        config.setClusterName(System.getenv().getOrDefault("HAZELCAST_CLUSTER_NAME", "SearchEngine"));
        String members = System.getenv().getOrDefault(
                "HZ_MEMBERS",
                "localhost:5701,localhost:5702,localhost:5703"
        );
        for (String member : members.split(",")) {
            config.getNetworkConfig().addAddress(member.trim());
        }
        return config;
    }
}
