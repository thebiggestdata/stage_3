package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.domain.gateway.Datalake;
import com.thebiggestdata.domain.entity.BookContent;

public class HazelcastDatalake implements Datalake {

    private final HazelcastInstance hz;
    private final HazelcastReplicationExecuter replicator;

    public HazelcastDatalake(HazelcastInstance hz, HazelcastReplicationExecuter replicator) {
        this.hz = hz;
        this.replicator = replicator;
    }

    @Override
    public void save(int bookId, BookContent content) {
        IMap<Integer, BookContent> map = hz.getMap("datalake");
        map.put(bookId, content);
    }

    @Override
    public void replicate(int bookId) {
        replicator.execute(bookId);
    }
}