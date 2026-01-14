package com.thebiggestdata.search.infrastructure.adapter;

import com.thebiggestdata.search.domain.port.InvertedIndexReaderPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

import java.util.ArrayList;
import java.util.List;

public class HazelcastInvertedIndexReaderAdapter implements InvertedIndexReaderPort {

    private final HazelcastInstance hazelcast;
    private final MultiMap<String, Integer> index;

    public HazelcastInvertedIndexReaderAdapter(HazelcastInstance hazelcast, String mapName) {
        this.hazelcast = hazelcast;
        this.index = hazelcast.getMultiMap(mapName);
    }

    @Override
    public List<Integer> getBookIdsForToken(String token) {
        var values = index.get(token);
        return values == null ? List.of() : new ArrayList<>(values);
    }
}
