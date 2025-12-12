package com.thebiggestdata.indexing.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.thebiggestdata.indexing.infrastructure.port.DatalakeReader;
import com.thebiggestdata.indexing.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class HazelcastDatalakeReader implements DatalakeReader {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastDatalakeReader.class);
    private final HazelcastInstance hazelcast;

    public HazelcastDatalakeReader(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public BookContent read(int bookId) {
        MultiMap<Integer, GenericRecord> datalake = hazelcast.getMultiMap("datalake");

        if (!datalake.containsKey(bookId)) {
            logger.warn("Book {} not found in datalake", bookId);
            return null;
        }

        Collection<GenericRecord> books = datalake.get(bookId);
        if (books == null || books.isEmpty()) {
            logger.warn("Book {} has no content in datalake", bookId);
            return null;
        }
        GenericRecord bookData = books.iterator().next();
        logger.info("Retrieved book {} from datalake (fields: {})", bookId, bookData.getFieldNames());
        try {
            String header = bookData.getString("header");
            String body = bookData.getString("body");
            if (body == null) {
                logger.warn("Book {} has null body", bookId);
                return null;
            }
            logger.debug("Successfully extracted book {} (header length: {}, body length: {})",
                    bookId, header != null ? header.length() : 0, body.length());
            return new BookContent(bookId, header, body);
        } catch (Exception e) {
            logger.error("Error reading book {} from GenericRecord: {}", bookId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean exists(int bookId) {
        MultiMap<Integer, GenericRecord> datalake = hazelcast.getMultiMap("datalake");
        return datalake.containsKey(bookId);
    }
}
