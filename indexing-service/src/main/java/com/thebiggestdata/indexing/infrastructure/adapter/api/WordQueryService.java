package com.thebiggestdata.indexing.infrastructure.adapter.api;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.indexing.infrastructure.port.WordQueryProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class WordQueryService implements WordQueryProvider {
    private final HazelcastInstance hazelcast;

    public WordQueryService(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<String, Object> query(String word) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");
        Collection<Integer> bookIds = invertedIndex.get(word.toLowerCase());

        return Map.of(
                "word", word,
                "count", bookIds.size(),
                "book_ids", new ArrayList<>(bookIds)
        );
    }
}