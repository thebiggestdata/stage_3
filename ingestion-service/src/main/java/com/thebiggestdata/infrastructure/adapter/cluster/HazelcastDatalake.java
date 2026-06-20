package com.thebiggestdata.infrastructure.adapter.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.domain.gateway.Datalake;
import com.thebiggestdata.domain.entity.BookText;

public class HazelcastDatalake implements Datalake {

    private final HazelcastInstance hz;
    private final HazelcastReplicationRunner replicator;

    public HazelcastDatalake(HazelcastInstance hz, HazelcastReplicationRunner replicator) {
        this.hz = hz;
        this.replicator = replicator;
    }

    @Override
    public void save(int bookId, BookText content) {
        IMap<Integer, BookText> map = hz.getMap("datalake");
        map.put(bookId, content);
    }

    @Override
    public void replicate(int bookId) {
        replicator.execute(bookId);
    }
}