package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingClaim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazelcastIndexAdaptersTest {

    private static HazelcastInstance hazelcast;

    @BeforeAll
    static void startHazelcast() {
        Config config = new Config().setClusterName("index-test-" + UUID.randomUUID());
        config.setProperty("hazelcast.logging.type", "none");
        config.getNetworkConfig().setPort(15901).setPortAutoIncrement(true);
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.addMapConfig(new MapConfig(HazelcastNames.INVERTED_INDEX + ":*"));
        hazelcast = Hazelcast.newHazelcastInstance(config);
    }

    @AfterAll
    static void stopHazelcast() {
        hazelcast.shutdown();
    }

    @Test
    void keepsGenerationsIsolatedAndDeduplicatesPostings() {
        HazelcastIndexGenerationStore generations = new HazelcastIndexGenerationStore(hazelcast);
        HazelcastInvertedIndex index = new HazelcastInvertedIndex(hazelcast, 2);
        IndexGeneration first = generations.active();
        IndexGeneration second = new IndexGeneration("second");

        try {
            index.addAll(first, List.of(new IndexedTerm("clean", 42, 2)));
            index.addAll(first, List.of(new IndexedTerm("clean", 42, 2)));
            index.addAll(second, List.of(new IndexedTerm("clean", 77, 1)));

            IMap<String, Set<String>> firstIndex = hazelcast.getMap("inverted-index:" + first.value());
            IMap<String, Set<String>> secondIndex = hazelcast.getMap("inverted-index:second");
            assertEquals(Set.of("42:2"), firstIndex.get("clean"));
            assertEquals(Set.of("77:1"), secondIndex.get("clean"));

            generations.activate(second);
            assertEquals(second, generations.active());
        } finally {
            index.close();
        }
    }

    @Test
    void mergesConcurrentPostingsForTheSameTerm() throws Exception {
        HazelcastInvertedIndex index = new HazelcastInvertedIndex(hazelcast, 4);
        IndexGeneration generation = new IndexGeneration("concurrent");

        try {
            CompletableFuture<Void> firstWrite = CompletableFuture.runAsync(() ->
                    index.addAll(generation, List.of(new IndexedTerm("shared", 1, 2))));
            CompletableFuture<Void> secondWrite = CompletableFuture.runAsync(() ->
                    index.addAll(generation, List.of(new IndexedTerm("shared", 2, 3))));

            CompletableFuture.allOf(firstWrite, secondWrite).get(2, TimeUnit.SECONDS);

            IMap<String, Set<String>> storedIndex = hazelcast.getMap("inverted-index:" + generation.value());
            assertEquals(Set.of("1:2", "2:3"), storedIndex.get("shared"));
        } finally {
            index.close();
        }
    }

    @Test
    void releasesFailedClaimsAndConfirmsCompletedBooks() {
        IndexGeneration generation = new IndexGeneration("claims");
        HazelcastIndexingTracker tracker = new HazelcastIndexingTracker(
                hazelcast,
                "node-a",
                Duration.ofMinutes(1)
        );

        assertEquals(IndexingClaim.ACQUIRED, tracker.claim(generation, 42));
        assertEquals(IndexingClaim.IN_PROGRESS, tracker.claim(generation, 42));
        tracker.release(generation, 42);
        assertEquals(IndexingClaim.ACQUIRED, tracker.claim(generation, 42));
        tracker.complete(generation, 42);
        assertEquals(IndexingClaim.ALREADY_INDEXED, tracker.claim(generation, 42));
    }

    @Test
    void removesDistributedRebuildStateWhenFinishing() {
        HazelcastRebuildCoordination coordination = new HazelcastRebuildCoordination(
                hazelcast,
                Duration.ofMinutes(1)
        );

        assertTrue(coordination.tryStart("first-rebuild"));
        coordination.prepare("first-rebuild", 1);
        coordination.fail("first-rebuild", "node-a", "simulated failure");
        coordination.finish("first-rebuild");

        assertTrue(hazelcast.<String, String>getMap(HazelcastNames.REBUILD_FAILURES).isEmpty());
        assertTrue(coordination.tryStart("second-rebuild"));
        coordination.finish("second-rebuild");
    }

    @Test
    void keepsIndexingConsumersWaitingUntilRebuildFinishes() throws Exception {
        HazelcastRebuildCoordination coordination = new HazelcastRebuildCoordination(
                hazelcast,
                Duration.ofMinutes(1)
        );
        HazelcastRebuildState rebuildState = new HazelcastRebuildState(hazelcast);
        assertTrue(coordination.tryStart("blocking-rebuild"));

        CompletableFuture<Void> waitingConsumer = CompletableFuture.runAsync(rebuildState::awaitCompletion);
        TimeUnit.MILLISECONDS.sleep(200);
        assertFalse(waitingConsumer.isDone());

        coordination.finish("blocking-rebuild");
        waitingConsumer.get(2, TimeUnit.SECONDS);
    }

    @Test
    void expiresAbandonedRebuildStateAfterLease() throws Exception {
        HazelcastRebuildCoordination coordination = new HazelcastRebuildCoordination(
                hazelcast,
                Duration.ofMillis(100)
        );

        assertTrue(coordination.tryStart("abandoned-rebuild"));
        TimeUnit.MILLISECONDS.sleep(300);
        assertTrue(coordination.tryStart("next-rebuild"));
        coordination.finish("next-rebuild");
    }
}
