package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.ClusterTopology;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.infrastructure.ports.IngestionControlPublisher;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.infrastructure.ports.PendingBookSeeder;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCommandPublisher;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCoordination;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingClaim;
import com.thebiggestdata.indexing.model.RebuildCommand;
import com.thebiggestdata.indexing.model.RebuildOutcome;
import com.thebiggestdata.indexing.model.RebuildResult;
import com.thebiggestdata.indexing.model.RecoveryResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RebuildIndexUseCaseTest {

    @Test
    void switchesGenerationOnlyAfterAllIndexersComplete() {
        List<String> operations = new ArrayList<>();
        RebuildIndexUseCase useCase = useCase(
                operations,
                new RebuildOutcome(true, 91, List.of())
        );

        RebuildResult result = useCase.execute();

        assertTrue(result.success());
        assertEquals(
                List.of(
                        "try-start", "pause", "prepare-generation", "prepare-coordination",
                        "publish", "await", "activate-generation", "reset-queue", "seed:91",
                        "resume", "finish"
                ),
                operations
        );
    }

    @Test
    void keepsCurrentGenerationWhenAnyIndexerFails() {
        List<String> operations = new ArrayList<>();
        RebuildIndexUseCase useCase = useCase(
                operations,
                new RebuildOutcome(true, 91, List.of("node-b: disk unavailable"))
        );

        RebuildResult result = useCase.execute();

        assertFalse(result.success());
        assertFalse(operations.contains("activate-generation"));
        assertFalse(operations.contains("resume"));
        assertTrue(operations.containsAll(List.of("clear-tracker", "clear-index", "clear-metadata", "clear-metrics")));
    }

    private RebuildIndexUseCase useCase(List<String> operations, RebuildOutcome outcome) {
        IngestionControlPublisher ingestion = new IngestionControlPublisher() {
            @Override public void pause() { operations.add("pause"); }
            @Override public void resume() { operations.add("resume"); }
        };
        InvertedIndex index = new InvertedIndex() {
            @Override public void addAll(IndexGeneration generation, List<IndexedTerm> terms) {}
            @Override public void clear(IndexGeneration generation) { operations.add("clear-index"); }
        };
        IndexingTracker tracker = new IndexingTracker() {
            @Override public IndexingClaim claim(IndexGeneration generation, int bookId) { return IndexingClaim.ACQUIRED; }
            @Override public void complete(IndexGeneration generation, int bookId) {}
            @Override public void release(IndexGeneration generation, int bookId) {}
            @Override public void clear(IndexGeneration generation) { operations.add("clear-tracker"); }
        };
        IndexGenerationStore generations = new IndexGenerationStore() {
            @Override public IndexGeneration active() { return new IndexGeneration("old"); }
            @Override public void prepare(IndexGeneration generation) { operations.add("prepare-generation"); }
            @Override public void activate(IndexGeneration generation) { operations.add("activate-generation"); }
        };
        MetadataStore metadata = new MetadataStore() {
            @Override public void save(IndexGeneration generation, int bookId, BookMetadata metadata) {}
            @Override public void clear(IndexGeneration generation) { operations.add("clear-metadata"); }
        };
        TokenMetrics metrics = new TokenMetrics() {
            @Override public void record(IndexGeneration generation, int bookId, int tokenCount) {}
            @Override public void clear(IndexGeneration generation) { operations.add("clear-metrics"); }
        };
        PendingBookSeeder pending = new PendingBookSeeder() {
            @Override public void seedAfter(int maxBookId) { operations.add("seed:" + maxBookId); }
            @Override public void reset() { operations.add("reset-queue"); }
        };
        RebuildCommandPublisher commands = command -> operations.add("publish");
        RebuildCoordination coordination = new RebuildCoordination() {
            @Override public boolean tryStart(String rebuildId) { operations.add("try-start"); return true; }
            @Override public void prepare(String rebuildId, int expectedIndexers) {
                operations.add("prepare-coordination");
            }
            @Override public void complete(String rebuildId, String nodeId, RecoveryResult result) {}
            @Override public void fail(String rebuildId, String nodeId, String reason) {}
            @Override public RebuildOutcome await(String rebuildId, Duration timeout) {
                operations.add("await");
                return outcome;
            }
            @Override public void finish(String rebuildId) { operations.add("finish"); }
        };
        ClusterTopology topology = new ClusterTopology() {
            @Override public int indexerCount() { return 2; }
            @Override public String localNodeId() { return "node-a"; }
        };
        return new RebuildIndexUseCase(
                ingestion,
                index,
                tracker,
                generations,
                metadata,
                metrics,
                pending,
                commands,
                coordination,
                topology,
                Duration.ofSeconds(5)
        );
    }
}
