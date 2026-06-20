package com.thebiggestdata.ingestion.model;

import java.util.List;

public record ReplicationResult(int bookId, int requestedReplicas,
                                List<String> successfulPeers, List<String> failedPeers) {
    public boolean isQuorumReached(){
        return failedPeers.size() >= ((requestedReplicas / 2) + 1);
    }
}
