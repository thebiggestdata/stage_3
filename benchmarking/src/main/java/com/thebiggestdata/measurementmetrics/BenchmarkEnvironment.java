package com.thebiggestdata.measurementmetrics;

import com.hazelcast.client.config.ClientConfig;

final class BenchmarkEnvironment {

    private static final int SAMPLE_SECONDS = 10;
    private static final long CLUSTER_STABLE_MS = 5_000;
    private static final long POLL_MS = 100;

    private BenchmarkEnvironment() {
    }

    static int sampleSeconds() {
        return SAMPLE_SECONDS;
    }

    static long stableMillis() {
        return CLUSTER_STABLE_MS;
    }

    static long pollMillis() {
        return POLL_MS;
    }

    static ClientConfig clientConfig() {
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
