package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.thebiggestdata.indexing.infrastructure.ports.BookStore;
import com.thebiggestdata.indexing.model.BookContent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastBookStore implements BookStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastBookStore.class);
    private final IMap<Integer, BookContent> datalake;

    public HazelcastBookStore(HazelcastInstance hazelcastInstance) {
        this.datalake = hazelcastInstance.getMap("datalake");
    }

    @Override
    public BookContent getBookContent(int bookId) {
        try {
            BookContent book = this.datalake.get(bookId);

            if (book == null) {
                log.error("Book {} not found in Hazelcast datalake", bookId);
                throw new RuntimeException("Book not found in Hazelcast: " + bookId);
            }

            return book;

        } catch (Exception e) {
            log.error("Error retrieving book {}: {}", bookId, e.getMessage());
            throw new RuntimeException("Error accessing Hazelcast", e);
        }
    }

    @Override
    public void save(int bookId, BookContent content) {
        this.datalake.put(bookId, content);
    }
}