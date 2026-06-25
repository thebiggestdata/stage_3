package com.thebiggestdata.search.model;

public record HealthStatus(boolean healthy, int indexedDocuments, String nodeId) {
}
