package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.infrastructure.ports.HealthCheck;
import com.thebiggestdata.search.infrastructure.ports.HealthProbe;
import com.thebiggestdata.search.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.search.model.IndexGeneration;
import com.thebiggestdata.search.model.HealthStatus;

public final class CheckHealthUseCase implements HealthCheck {

    private final IndexGenerationStore generations;
    private final HealthProbe probe;

    public CheckHealthUseCase(IndexGenerationStore generations, HealthProbe probe) {
        this.generations = generations;
        this.probe = probe;
    }

    @Override
    public HealthStatus check() {
        IndexGeneration generation = generations.active();
        return new HealthStatus(true, probe.indexedDocumentCount(generation), probe.localNodeId());
    }
}
