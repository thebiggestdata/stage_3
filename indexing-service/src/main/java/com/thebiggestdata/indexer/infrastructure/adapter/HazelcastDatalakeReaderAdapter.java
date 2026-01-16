package com.thebiggestdata.indexer.infrastructure.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.port.DatalakeReadPort;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class HazelcastDatalakeReaderAdapter implements DatalakeReadPort {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDatalakeReaderAdapter.class);
    private final HazelcastInstance hazelcast;

    public HazelcastDatalakeReaderAdapter(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public RawDocument read(int bookId, String bodyPath) {
        logger.info("Reading book {} from Hazelcast RAM", bookId);

        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");
        Collection<DuplicatedBook> books = datalake.get(bookId);

        if (books == null || books.isEmpty()) {
            throw new RuntimeException("Book " + bookId + " not found in Hazelcast RAM");
        }

        DuplicatedBook book = books.iterator().next();
        logger.info("Book {} retrieved from Hazelcast RAM", bookId);

        return new RawDocument(bookId, book.body());
    }
}
