package com.thebiggestdata.measurementmetrics;

import com.hazelcast.client.HazelcastClient;
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
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(BenchmarkEnvironment.clientConfig());

        try {
            List<Double> tokenRates = new ArrayList<>();
            List<Double> bookRates = new ArrayList<>();
            int warmupIterations = 5;
            int measurementIterations = 10;
            int sampleSeconds = BenchmarkEnvironment.sampleSeconds();

            for (int iteration = 0; iteration < warmupIterations + measurementIterations; iteration++) {
                String generation = activeGeneration(hazelcast);
                IMap<Integer, Integer> tokenCounts = tokenCounts(hazelcast, generation);
                long startTokens = totalTokens(tokenCounts);
                int startBooks = tokenCounts.size();
                long startTime = System.nanoTime();
                Thread.sleep(sampleSeconds * 1_000L);

                if (!generation.equals(activeGeneration(hazelcast))) {
                    throw new IllegalStateException("Index generation changed during the benchmark");
                }
                long endTokens = totalTokens(tokenCounts);
                int endBooks = tokenCounts.size();
                double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                double tokensPerSecond = (endTokens - startTokens) / seconds;
                double booksPerSecond = (endBooks - startBooks) / seconds;

                boolean warmup = iteration < warmupIterations;
                int phaseIteration = warmup ? iteration + 1 : iteration - warmupIterations + 1;
                if (!warmup) {
                    tokenRates.add(tokensPerSecond);
                    bookRates.add(booksPerSecond);
                }
                System.out.printf("%s %2d: %.1f tokens/s, %.3f docs/s (%.1fs) [generation=%s, tokens=%d, docs=%d]%n",
                        warmup ? "warmup" : "iter",
                        phaseIteration,
                        tokensPerSecond,
                        booksPerSecond,
                        seconds,
                        generation,
                        endTokens - startTokens,
                        endBooks - startBooks);
            }

            IngestionRate.printResults("IndexingThroughput", "tokens/s", tokenRates);
            IngestionRate.printResults("IndexedDocuments", "docs/s", bookRates);
        } finally {
            hazelcast.shutdown();
        }
    }

    private static String activeGeneration(HazelcastInstance hazelcast) {
        IMap<String, String> generations = hazelcast.getMap(INDEX_GENERATIONS);
        return generations.getOrDefault("active", "initial");
    }

    private static IMap<Integer, Integer> tokenCounts(HazelcastInstance hazelcast, String generation) {
        return hazelcast.getMap(TOKEN_COUNTS + ":" + generation);
    }

    private static long totalTokens(IMap<Integer, Integer> tokenCounts) {
        return tokenCounts.values().stream().mapToLong(Integer::longValue).sum();
    }
}
