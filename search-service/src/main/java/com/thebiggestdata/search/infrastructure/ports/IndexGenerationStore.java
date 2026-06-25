package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.IndexGeneration;

public interface IndexGenerationStore {

    IndexGeneration active();
}
