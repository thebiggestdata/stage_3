package com.thebiggestdata;

import com.thebiggestdata.metrics.IngestionRate;
import com.thebiggestdata.metrics.RecoveryTime;
import com.thebiggestdata.metrics.IndexingThroughput;

public class Runner {
    public static void main(String[] args) throws Exception {
        String mode = System.getenv("BENCHMARK_MODE");

        if (mode == null || mode.isBlank()) {
            mode = "recoverytime";
            System.out.println(">>> No BENCHMARK_MODE found. Defaulting to: " + mode);
        }

        System.out.println("==========================================");
        System.out.println("   STARTING BENCHMARK: " + mode.toUpperCase());
        System.out.println("==========================================");

        switch (mode.toLowerCase().trim()) {
            case "ingestionrate":
                IngestionRate.main(args);
                break;
            case "indexingthroughput":
                IndexingThroughput.main(args);
                break;
            case "recoverytime":
                RecoveryTime.main(args);
                break;
            default:
                System.err.println("!!! Unknown mode: " + mode);
                System.err.println("Available modes: ingestion, indexing, recovery");
                System.exit(1);
        }
    }
}