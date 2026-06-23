package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestionGuard;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class HazelcastBookIngestionGuard implements BookIngestionGuard {

    private final String localNodeId;
    private final Duration lease;
    private final IMap<Integer, String> owners;

    public HazelcastBookIngestionGuard(HazelcastInstance hazelcast, String localNodeId, Duration lease) {
        this.localNodeId = Objects.requireNonNull(localNodeId, "localNodeId");
        this.lease = Objects.requireNonNull(lease, "lease");
        this.owners = hazelcast.getMap(HazelcastNames.INGESTIONS_IN_PROGRESS);
    }

    @Override
    public boolean tryAcquire(int bookId) {
        String previousOwner = owners.putIfAbsent(
                bookId,
                localNodeId,
                lease.toMillis(),
                TimeUnit.MILLISECONDS
        );
        return previousOwner == null;
    }

    @Override
    public void release(int bookId) {
        owners.remove(bookId, localNodeId);
    }
}
