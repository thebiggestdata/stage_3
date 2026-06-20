package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.thebiggestdata.domain.gateway.BookRepository;
import com.thebiggestdata.domain.entity.BookText;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastBookRepository implements BookRepository {
    private static final Logger log = LoggerFactory.getLogger(HazelcastBookRepository.class);
    private final IMap<Integer, BookText> datalake;

    public HazelcastBookRepository(HazelcastInstance hazelcastInstance) {
        this.datalake = hazelcastInstance.getMap("datalake");
    }

    @Override
    public BookText getBookContent(int bookId) {
        try {
            BookText book = this.datalake.get(bookId);

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
    public void save(int bookId, BookText content) {
        this.datalake.put(bookId, content);
    }
}