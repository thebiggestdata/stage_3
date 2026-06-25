package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.IndexGeneration;

public interface HealthProbe {

    int indexedDocumentCount(IndexGeneration generation);

    String localNodeId();
}
