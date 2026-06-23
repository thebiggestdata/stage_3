package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.BookContent;

public final class HazelcastBookContentStore implements BookContentStore {

    private final IMap<Integer, BookContent> books;

    public HazelcastBookContentStore(HazelcastInstance hazelcast) {
        this.books = hazelcast.getMap(HazelcastNames.DATALAKE);
    }

    @Override
    public BookContent get(int bookId) {
        BookContent content = books.get(bookId);
        if (content == null) {
            throw new HazelcastAdapterException("Book not found in live datalake: " + bookId);
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
