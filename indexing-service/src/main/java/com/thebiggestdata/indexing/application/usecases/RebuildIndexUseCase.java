package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.ClusterTopology;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.ports.IngestionControlPublisher;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.infrastructure.ports.PendingBookSeeder;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCommandPublisher;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCoordination;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.RebuildCommand;
import com.thebiggestdata.indexing.model.RebuildOutcome;
import com.thebiggestdata.indexing.model.RebuildResult;
import com.thebiggestdata.indexing.model.IndexGeneration;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class RebuildIndexUseCase {

    private final IngestionControlPublisher ingestionControl;
    private final InvertedIndex index;
    private final IndexingTracker tracker;
    private final IndexGenerationStore generations;
    private final MetadataStore metadata;
    private final TokenMetrics tokenMetrics;
    private final PendingBookSeeder pendingBooks;
    private final RebuildCommandPublisher commands;
    private final RebuildCoordination coordination;
    private final ClusterTopology topology;
    private final Duration timeout;

    public RebuildIndexUseCase(
            IngestionControlPublisher ingestionControl,
            InvertedIndex index,
            IndexingTracker tracker,
            IndexGenerationStore generations,
            MetadataStore metadata,
            TokenMetrics tokenMetrics,
            PendingBookSeeder pendingBooks,
            RebuildCommandPublisher commands,
            RebuildCoordination coordination,
            ClusterTopology topology,
            Duration timeout
    ) {
        this.ingestionControl = ingestionControl;
        this.index = index;
        this.tracker = tracker;
        this.generations = generations;
        this.metadata = metadata;
        this.tokenMetrics = tokenMetrics;
        this.pendingBooks = pendingBooks;
        this.commands = commands;
        this.coordination = coordination;
        this.topology = topology;
        this.timeout = timeout;
    }

    public RebuildResult execute() {
        String rebuildId = UUID.randomUUID().toString();
        if (!coordination.tryStart(rebuildId)) {
            return new RebuildResult(false, rebuildId, "Another rebuild is already active");
        }

        try {
            ingestionControl.pause();
            IndexGeneration target = new IndexGeneration(rebuildId);
            generations.prepare(target);
            coordination.prepare(rebuildId, topology.indexerCount());
            commands.publish(new RebuildCommand(rebuildId, target, Instant.now().toEpochMilli()));

            RebuildOutcome outcome = coordination.await(rebuildId, timeout);
            if (!outcome.successful()) {
                clearGeneration(target);
                return new RebuildResult(false, rebuildId, failureMessage(outcome));
            }

            generations.activate(target);
            pendingBooks.reset();
            pendingBooks.seedAfter(outcome.maxBookId());
            ingestionControl.resume();
            return new RebuildResult(true, rebuildId, "Rebuilt index on all indexer nodes");
        } catch (RuntimeException e) {
            return new RebuildResult(false, rebuildId, message(e));
        } finally {
            coordination.finish(rebuildId);
        }
    }

    private void clearGeneration(IndexGeneration generation) {
        tracker.clear(generation);
        index.clear(generation);
        metadata.clear(generation);
        tokenMetrics.clear(generation);
    }

    private String failureMessage(RebuildOutcome outcome) {
        if (!outcome.completed()) {
            return "Rebuild timed out; ingestion remains paused";
        }
        return "Rebuild failed: " + String.join("; ", outcome.failures());
    }

    private String message(RuntimeException exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
