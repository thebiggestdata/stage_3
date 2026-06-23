package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexGeneration;

public interface IndexGenerationStore {

    IndexGeneration active();

    void prepare(IndexGeneration generation);

    void activate(IndexGeneration generation);
}
