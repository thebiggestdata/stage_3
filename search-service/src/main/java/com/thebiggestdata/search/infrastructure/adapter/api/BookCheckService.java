package com.thebiggestdata.search.infrastructure.adapter.api;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.search.infrastructure.port.BookCheckProvider;
import java.util.Map;

public class BookCheckService implements BookCheckProvider {
    private final HazelcastInstance hazelcast;

    public BookCheckService(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<String, Object> check(int bookId) {
        IMap<Integer, Boolean> indexedBooks = hazelcast.getMap("indexed-books");
        boolean isIndexed = indexedBooks.containsKey(bookId);
        return Map.of(
                "book_id", bookId,
                "indexed", isIndexed
        );
    }
}