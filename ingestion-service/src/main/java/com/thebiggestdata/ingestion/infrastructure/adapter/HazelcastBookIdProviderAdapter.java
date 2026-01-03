package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.port.BookIdProviderPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;

public class HazelcastBookIdProviderAdapter implements BookIdProviderPort {

    private final IAtomicLong counter;

    public HazelcastBookIdProviderAdapter(HazelcastInstance hazelcast) {
        this.counter = hazelcast.getCPSubsystem().getAtomicLong("book-id-gen");
    }

    @Override
    public int nextBookId() {
        return (int) counter.getAndIncrement();
    }
}