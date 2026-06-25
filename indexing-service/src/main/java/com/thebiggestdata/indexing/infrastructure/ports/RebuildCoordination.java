package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.RebuildOutcome;
import com.thebiggestdata.indexing.model.RecoveryResult;

import java.time.Duration;

public interface RebuildCoordination {

    boolean tryStart(String rebuildId);

    void prepare(String rebuildId, int expectedIndexers);

    void complete(String rebuildId, String nodeId, RecoveryResult result);

    void fail(String rebuildId, String nodeId, String reason);

    RebuildOutcome await(String rebuildId, Duration timeout);

    void finish(String rebuildId);
}
