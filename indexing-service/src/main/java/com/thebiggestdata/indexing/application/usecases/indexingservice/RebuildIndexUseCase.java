package com.thebiggestdata.indexing.application.usecases.indexingservice;

import com.thebiggestdata.indexing.infrastructure.ports.IngestionControlPublisherPort;
import com.thebiggestdata.indexing.infrastructure.ports.RecoveryPort;
import com.thebiggestdata.indexing.model.RebuildCommand;
import com.thebiggestdata.indexing.model.RebuildResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebuildIndexUseCase { //TODO should implement something

    private static final Logger log = LoggerFactory.getLogger(RebuildIndexUseCase.class);

    private final RecoveryPort recovery;
    private final IngestionControlPublisherPort ingestionControl;

    public RebuildIndexUseCase(RecoveryPort recovery, IngestionControlPublisherPort ingestionControl) {
        this.recovery = recovery;
        this.ingestionControl = ingestionControl;
    }

    public RebuildResult rebuildIndex(RebuildCommand command) {
        log.info("Rebuilding request at epoch = {}", command.epoch());

        try {
            ingestionControl.publishPause();
            int rebuilt = recovery.executeRecovery();
            ingestionControl.publishResume();
            return new RebuildResult(true, "Rebuilt " + rebuilt + " documents");
        } catch (Exception e) {
            ingestionControl.publishPause();
            return new RebuildResult(false, e.getMessage());

        }
    }
}
