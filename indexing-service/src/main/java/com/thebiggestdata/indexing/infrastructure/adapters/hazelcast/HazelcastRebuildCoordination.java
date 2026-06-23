package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildCoordination;
import com.thebiggestdata.indexing.model.RebuildOutcome;
import com.thebiggestdata.indexing.model.RecoveryResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class HazelcastRebuildCoordination implements RebuildCoordination {

    private final IMap<String, String> state;
    private final IMap<String, Integer> expected;
    private final IMap<String, Set<String>> completions;
    private final IMap<String, Integer> maxBookIds;
    private final IMap<String, String> failures;

    public HazelcastRebuildCoordination(HazelcastInstance hazelcast) {
        this.state = hazelcast.getMap(HazelcastNames.REBUILD_STATE);
        this.expected = hazelcast.getMap(HazelcastNames.REBUILD_EXPECTED);
        this.completions = hazelcast.getMap(HazelcastNames.REBUILD_COMPLETIONS);
        this.maxBookIds = hazelcast.getMap(HazelcastNames.REBUILD_MAX_BOOK_IDS);
        this.failures = hazelcast.getMap(HazelcastNames.REBUILD_FAILURES);
    }

    @Override
    public boolean tryStart(String rebuildId) {
        return state.putIfAbsent(HazelcastRebuildState.ACTIVE_KEY, rebuildId) == null;
    }

    @Override
    public void prepare(String rebuildId, int expectedIndexers) {
        if (expectedIndexers < 1) {
            throw new IllegalStateException("No indexer members are available");
        }
        expected.put(rebuildId, expectedIndexers);
        completions.put(rebuildId, Set.of());
        maxBookIds.put(rebuildId, 0);
    }

    @Override
    public void complete(String rebuildId, String nodeId, RecoveryResult result) {
        maxBookIds.lock(rebuildId);
        try {
            maxBookIds.put(rebuildId, Math.max(maxBookIds.getOrDefault(rebuildId, 0), result.maxBookId()));
        } finally {
            maxBookIds.unlock(rebuildId);
        }
        if (!result.successful()) {
            failures.put(failureKey(rebuildId, nodeId), result.failedBooks() + " books failed");
        }
        recordCompletion(rebuildId, nodeId);
    }

    @Override
    public void fail(String rebuildId, String nodeId, String reason) {
        failures.put(failureKey(rebuildId, nodeId), reason);
        recordCompletion(rebuildId, nodeId);
    }

    private void recordCompletion(String rebuildId, String nodeId) {
        completions.lock(rebuildId);
        try {
            Set<String> nodes = new HashSet<>(completions.getOrDefault(rebuildId, Set.of()));
            nodes.add(nodeId);
            completions.put(rebuildId, nodes);
        } finally {
            completions.unlock(rebuildId);
        }
    }

    @Override
    public RebuildOutcome await(String rebuildId, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        int expectedIndexers = expected.getOrDefault(rebuildId, 0);
        while (Instant.now().isBefore(deadline)) {
            if (completions.getOrDefault(rebuildId, Set.of()).size() >= expectedIndexers) {
                return outcome(rebuildId, true);
            }
            sleep();
        }
        return outcome(rebuildId, false);
    }

    private RebuildOutcome outcome(String rebuildId, boolean completed) {
        List<String> failureMessages = new ArrayList<>();
        String prefix = rebuildId + ":";
        for (Map.Entry<String, String> failure : failures.entrySet()) {
            if (failure.getKey().startsWith(prefix)) {
                failureMessages.add(failure.getKey().substring(prefix.length()) + ": " + failure.getValue());
            }
        }
        return new RebuildOutcome(
                completed,
                maxBookIds.getOrDefault(rebuildId, 0),
                failureMessages
        );
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HazelcastAdapterException("Interrupted while waiting for rebuild", e);
        }
    }

    @Override
    public void finish(String rebuildId) {
        state.remove(HazelcastRebuildState.ACTIVE_KEY, rebuildId);
        expected.remove(rebuildId);
        completions.remove(rebuildId);
        maxBookIds.remove(rebuildId);
        removeFailuresFor(rebuildId);
    }

    private void removeFailuresFor(String rebuildId) {
        String prefix = rebuildId + ":";
        for (String key : List.copyOf(failures.keySet())) {
            if (key.startsWith(prefix)) {
                failures.remove(key);
            }
        }
    }

    private String failureKey(String rebuildId, String nodeId) {
        return rebuildId + ":" + nodeId;
    }
}
