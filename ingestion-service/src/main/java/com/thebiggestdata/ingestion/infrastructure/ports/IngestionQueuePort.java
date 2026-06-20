package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.Node;

import java.util.List;

public interface IngestionQueuePort {
    Integer pollNext();
    boolean isBookIndexed(int bookId);
    int getDatalakeSize();
    List<Node> getIndexerNodeCount();
}