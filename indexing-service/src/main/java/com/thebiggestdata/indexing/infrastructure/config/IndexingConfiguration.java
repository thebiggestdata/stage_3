package com.thebiggestdata.indexing.infrastructure.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public record IndexingConfiguration(
        Path datalakeRoot,
        String brokerUrl,
        String hazelcastClusterName,
        int servicePort,
        int eventConsumers,
        int eventPrefetch,
        int maxRedeliveries,
        int indexWriters,
        Duration indexingClaimLease,
        Duration rebuildTimeout,
        int lastBookId
) {

    public static IndexingConfiguration load(String[] arguments, Map<String, String> environment) {
        int processors = Runtime.getRuntime().availableProcessors();
        return new IndexingConfiguration(
                Path.of(arguments.length == 0 ? "datalake" : arguments[0]),
                value(environment, "BROKER_URL", "tcp://localhost:61616"),
                value(environment, "HAZELCAST_CLUSTER_NAME", "SearchEngine"),
                positiveInt(environment, "SERVICE_PORT", 7002),
                positiveInt(environment, "INDEXING_CONSUMERS", Math.max(1, processors)),
                positiveInt(environment, "ACTIVEMQ_PREFETCH", 100),
                nonNegativeInt(environment, "ACTIVEMQ_MAX_REDELIVERIES", 5),
                positiveInt(environment, "INDEX_WRITERS", Math.max(1, processors)),
                duration(environment, "INDEXING_CLAIM_LEASE_MS", 300_000),
                duration(environment, "REBUILD_TIMEOUT_MS", 600_000),
                positiveInt(environment, "LAST_BOOK_ID", 100_000)
        );
    }

    private static String value(Map<String, String> environment, String name, String defaultValue) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int positiveInt(Map<String, String> environment, String name, int defaultValue) {
        int value = integer(environment, name, defaultValue);
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static int nonNegativeInt(Map<String, String> environment, String name, int defaultValue) {
        int value = integer(environment, name, defaultValue);
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static int integer(Map<String, String> environment, String name, int defaultValue) {
        return Integer.parseInt(value(environment, name, Integer.toString(defaultValue)));
    }

    private static Duration duration(Map<String, String> environment, String name, long defaultMillis) {
        long millis = Long.parseLong(value(environment, name, Long.toString(defaultMillis)));
        if (millis < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return Duration.ofMillis(millis);
    }
}
