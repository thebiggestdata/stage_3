package com.thebiggestdata.ingestion.infrastructure.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public record IngestionConfiguration(
        Path datalakeRoot,
        String brokerUrl,
        String hazelcastClusterName,
        Optional<String> nodeId,
        int replicationFactor,
        int indexingBufferFactor,
        int ingestionWorkers,
        int servicePort,
        Duration providerTimeout,
        int providerRetries,
        Duration providerRetryDelay,
        Duration pendingBookPollTimeout,
        int pendingBookMaxAttempts,
        Duration replicationTimeout,
        Duration replicationCheckInterval,
        Duration ingestionLease,
        Duration schedulerInterval
) {

    public static IngestionConfiguration load(String[] arguments, Map<String, String> environment) {
        Path datalakeRoot = Path.of(arguments.length == 0 ? "datalake" : arguments[0]);
        return new IngestionConfiguration(
                datalakeRoot,
                value(environment, "BROKER_URL", "tcp://localhost:61616"),
                value(environment, "HAZELCAST_CLUSTER_NAME", "SearchEngine"),
                nodeId(environment),
                positiveInt(environment, "REPLICATION_FACTOR", 1),
                positiveInt(environment, "INDEXING_BUFFER_FACTOR", 20),
                positiveInt(environment, "INGESTION_WORKERS", 4),
                positiveInt(environment, "SERVICE_PORT", 7001),
                positiveDuration(environment, "GUTENBERG_TIMEOUT_MS", 15_000),
                nonNegativeInt(environment, "GUTENBERG_MAX_RETRIES", 3),
                positiveDuration(environment, "GUTENBERG_RETRY_DELAY_MS", 500),
                positiveDuration(environment, "PENDING_BOOK_POLL_MS", 100),
                positiveInt(environment, "PENDING_BOOK_MAX_ATTEMPTS", 3),
                positiveDuration(environment, "REPLICATION_TIMEOUT_MS", 30_000),
                positiveDuration(environment, "REPLICATION_CHECK_INTERVAL_MS", 100),
                positiveDuration(environment, "INGESTION_LEASE_MS", 300_000),
                positiveDuration(environment, "INGESTION_CYCLE_INTERVAL_MS", 100)
        );
    }

    private static Optional<String> nodeId(Map<String, String> environment) {
        String explicitNodeId = environment.get("HZ_NODE_ID");
        if (explicitNodeId != null && !explicitNodeId.isBlank()) {
            return Optional.of(explicitNodeId);
        }
        return Optional.ofNullable(environment.get("HZ_PUBLIC_ADDRESS"))
                .filter(address -> !address.isBlank());
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
        String raw = environment.get(name);
        return raw == null || raw.isBlank() ? defaultValue : Integer.parseInt(raw);
    }

    private static Duration positiveDuration(
            Map<String, String> environment,
            String name,
            long defaultMillis
    ) {
        long millis = Long.parseLong(value(environment, name, Long.toString(defaultMillis)));
        if (millis < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return Duration.ofMillis(millis);
    }
}
