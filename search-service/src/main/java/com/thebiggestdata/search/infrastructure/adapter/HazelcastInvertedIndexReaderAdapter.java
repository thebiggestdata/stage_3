package com.thebiggestdata.search.infrastructure.adapter;

import com.thebiggestdata.search.domain.port.InvertedIndexReaderPort;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HazelcastInvertedIndexReaderAdapter implements InvertedIndexReaderPort {
    private final HazelcastInstance hazelcast;
    private final String mapName;

    public HazelcastInvertedIndexReaderAdapter(HazelcastInstance hazelcast, String mapName) {
        this.hazelcast = hazelcast;
        this.mapName = mapName;
    }

    @Override
    public List<Integer> getBookIdsForToken(String token) {
        MultiMap<String, String> mm = hazelcast.getMultiMap(mapName);
        Collection<String> values = mm.get(token);
        if (values == null || values.isEmpty()) return List.of();
        List<Integer> ids = new ArrayList<>(values.size());
        for (String v : values) {
            try {ids.add(Integer.parseInt(v));}
            catch (NumberFormatException ignored) { }
        }
        return ids;
    }
}