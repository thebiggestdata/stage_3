package com.thebiggestdata.ingestion.model;

import java.util.List;
import java.util.Objects;

public record ReplicationResult(
        int bookId,
        int requiredReplicas,
        List<String> replicaNodeIds) {

    public ReplicationResult {
        if (requiredReplicas < 1) {
            throw new IllegalArgumentException("requiredReplicas must be positive");
        }
        replicaNodeIds = List.copyOf(Objects.requireNonNull(replicaNodeIds, "replicaNodeIds"));
    }

    public boolean isSatisfied() {
        return replicaNodeIds.size() >= requiredReplicas;
    }
}
