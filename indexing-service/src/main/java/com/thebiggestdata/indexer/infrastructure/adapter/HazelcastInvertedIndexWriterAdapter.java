package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.InvertedIndexWriterPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import java.util.List;
import java.util.Map;

public class HazelcastInvertedIndexWriterAdapter implements InvertedIndexWriterPort {
    private final HazelcastInstance hazelcast;
    private final String mapName;

    public HazelcastInvertedIndexWriterAdapter(HazelcastInstance hazelcast, String mapName) {
        this.hazelcast = hazelcast;
        this.mapName = mapName;
    }

    @Override
    public void write(Map<String, List<Integer>> postings) {
        MultiMap<String, Integer> mm = hazelcast.getMultiMap(mapName);
        postings.forEach((token, ids) -> {for (Integer id : ids) mm.put(token, id);});
    }
}
