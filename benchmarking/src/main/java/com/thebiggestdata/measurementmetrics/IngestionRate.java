package com.thebiggestdata.measurementmetrics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class IngestionRate {

    private static final String DOWNLOADED_BOOKS = "downloaded-books";

    private IngestionRate() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        HazelcastInstance hazelcast = HazelcastClient.newHazelcastClient(BenchmarkEnvironment.clientConfig());

        try {
            ISet<Integer> downloadedBooks = hazelcast.getSet(DOWNLOADED_BOOKS);
            List<Double> rates = new ArrayList<>();
            int warmupIterations = 5;
            int measurementIterations = 10;
            int sampleSeconds = BenchmarkEnvironment.sampleSeconds();

            for (int iteration = 0; iteration < warmupIterations + measurementIterations; iteration++) {
                long startCount = downloadedBooks.size();
                long startTime = System.nanoTime();
                Thread.sleep(TimeUnit.SECONDS.toMillis(sampleSeconds));
                long endCount = downloadedBooks.size();
                double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                double rate = (endCount - startCount) / seconds;

                boolean warmup = iteration < warmupIterations;
                int phaseIteration = warmup ? iteration + 1 : iteration - warmupIterations + 1;
                if (!warmup) {
                    rates.add(rate);
                }
                System.out.printf("%s %2d: %.3f docs/s (%.1fs)%n",
                        warmup ? "warmup" : "iter", phaseIteration, rate, seconds);
            }

            printResults("IngestionRate", "docs/s", rates);
        } finally {
            hazelcast.shutdown();
        }
    }

    static void printResults(String name, String unit, List<Double> rates) {
        double average = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double deviation = Math.sqrt(rates.stream()
                .mapToDouble(rate -> Math.pow(rate - average, 2))
                .average()
                .orElse(0));

        System.out.println("\n=== FINAL RESULTS ===");
        System.out.printf("%s: %.3f +/- %.3f %s%n", name, average, deviation, unit);
        System.out.printf("(min=%.3f, avg=%.3f, max=%.3f)%n",
                rates.stream().min(Double::compareTo).orElse(0.0),
                average,
                rates.stream().max(Double::compareTo).orElse(0.0));
    }
}
