package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentNotFoundException;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HazelcastBookContentStore implements BookContentStore {

    private static final Logger log = LoggerFactory.getLogger(HazelcastBookContentStore.class);

    private final IMap<Integer, BookContent> books;

    public HazelcastBookContentStore(HazelcastInstance hazelcast) {
        this.books = hazelcast.getMap(HazelcastNames.DATALAKE);
    }

    @Override
    public BookContent get(int bookId) {
        BookContent content = books.get(bookId);
        if (content == null) {
            log.warn("INDEXING_DATALAKE_MISS bookId={} map={}", bookId, books.getName());
            throw new BookContentNotFoundException("Book not found in live datalake: " + bookId);
        }
        return content;
    }

    @Override
    public void save(int bookId, BookContent content) {
        books.put(bookId, content);
    }

    @Override
    public void remove(int bookId) {
        books.remove(bookId);
    }
}
