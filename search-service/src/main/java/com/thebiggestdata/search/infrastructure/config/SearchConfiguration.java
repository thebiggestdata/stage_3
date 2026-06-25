package com.thebiggestdata.search.infrastructure.config;

import java.util.Map;

public record SearchConfiguration(
        String hazelcastClusterName,
        int servicePort,
        int searchThreads,
        SortOrder sortOrder
) {

    public enum SortOrder {
        FREQUENCY,
        ID
    }

    public static SearchConfiguration load(Map<String, String> environment) {
        return new SearchConfiguration(
                value(environment, "HAZELCAST_CLUSTER_NAME", "SearchEngine"),
                positiveInt(environment, "SERVICE_PORT", 7003),
                positiveInt(environment, "SEARCH_THREADS", Math.max(1, Runtime.getRuntime().availableProcessors())),
                sortOrder(value(environment, "SORTING_CRITERIA", "frequency"))
        );
    }

    private static SortOrder sortOrder(String value) {
        try {
            return SortOrder.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SORTING_CRITERIA must be 'frequency' or 'id'", e);
        }
    }

    private static int positiveInt(Map<String, String> environment, String name, int defaultValue) {
        int value = Integer.parseInt(value(environment, name, Integer.toString(defaultValue)));
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String value(Map<String, String> environment, String name, String defaultValue) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
