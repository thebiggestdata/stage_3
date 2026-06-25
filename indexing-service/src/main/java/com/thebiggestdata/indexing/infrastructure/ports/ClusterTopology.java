package com.thebiggestdata.indexing.infrastructure.ports;

public interface ClusterTopology {

    int indexerCount();

    String localNodeId();
}
