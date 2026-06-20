package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.Datalake;
import com.thebiggestdata.ingestion.model.BookContent;

public class HazelcastDatalake implements Datalake {

    private final HazelcastInstance hz;
    private final HazelcastReplicationExecuter replicator;

    public HazelcastDatalake(HazelcastInstance hz, HazelcastReplicationExecuter replicator) {
        this.hz = hz;
        this.replicator = replicator;
    }

    @Override
    public void save(int bookId, BookContent bookContent) {
        IMap<Integer, BookContent> map = hz.getMap("datalake");
        map.put(bookId, bookContent);
    }

    @Override
    public void replicate(int bookId) {
        replicator.execute(bookId);
    }
}