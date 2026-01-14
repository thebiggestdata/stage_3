package com.thebiggestdata.search.infrastructure.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.search.domain.port.InvertedIndexReaderPort;

import java.util.HashSet;
import java.util.Set;

public class HazelcastInvertedIndexReaderAdapter implements InvertedIndexReaderPort {

    private final IMap<String, Set<String>> invertedIndex;

    public HazelcastInvertedIndexReaderAdapter(HazelcastInstance hazelcastInstance) {
        this.invertedIndex = hazelcastInstance.getMap("inverted-index");
    }

    @Override
    public Set<String> getDocumentsForTerm(String term) {
        Set<String> result = invertedIndex.get(term);
        return result != null ? new HashSet<>(result) : new HashSet<>();
    }
}