package com.thebiggestdata;

import com.thebiggestdata.measurementmetrics.IndexingThroughput;
import com.thebiggestdata.measurementmetrics.IngestionRate;
import com.thebiggestdata.measurementmetrics.RecoveryTime;

public final class BenchmarkRunner {

    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        String mode = System.getenv().getOrDefault("BENCHMARK_MODE", "recoverytime")
                .toLowerCase()
                .trim();

        System.out.println("==========================================");
        System.out.println("   STARTING BENCHMARK: " + mode.toUpperCase());
        System.out.println("==========================================");

        switch (mode) {
            case "ingestionrate", "ingestion-rate", "ingestion" -> IngestionRate.main(args);
            case "indexingthroughput", "indexing-throughput", "indexing" -> IndexingThroughput.main(args);
            case "recoverytime", "recovery-time", "recovery" -> RecoveryTime.main(args);
            default -> throw new IllegalArgumentException(
                    "Unknown BENCHMARK_MODE: " + mode
                            + ". Use ingestionrate, indexingthroughput or recoverytime"
            );
        }
    }
}
