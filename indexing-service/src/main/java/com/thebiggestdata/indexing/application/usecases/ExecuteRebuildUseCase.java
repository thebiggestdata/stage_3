package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.ClusterTopology;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCoordination;
import com.thebiggestdata.indexing.model.RebuildCommand;
import com.thebiggestdata.indexing.model.RecoveryResult;

public final class ExecuteRebuildUseCase {

    private final RecoverIndexUseCase recoverIndex;
    private final RebuildCoordination coordination;
    private final ClusterTopology topology;

    public ExecuteRebuildUseCase(
            RecoverIndexUseCase recoverIndex,
            RebuildCoordination coordination,
            ClusterTopology topology
    ) {
        this.recoverIndex = recoverIndex;
        this.coordination = coordination;
        this.topology = topology;
    }

    public void execute(RebuildCommand command) {
        try {
            RecoveryResult result = recoverIndex.execute(command.targetGeneration());
            coordination.complete(command.rebuildId(), topology.localNodeId(), result);
        } catch (RuntimeException e) {
            coordination.fail(command.rebuildId(), topology.localNodeId(), message(e));
        }
    }

    private String message(RuntimeException exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
