package com.thebiggestdata.infrastructure.adapter.cluster;

import com.thebiggestdata.domain.gateway.MetadataRepository;
import com.thebiggestdata.domain.entity.BookText;
import com.thebiggestdata.domain.entity.BookInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastMetadataRepository implements MetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(HazelcastMetadataRepository.class);
    private final MetadataReader parser;
    private final IMap<Integer, BookText> datalake;
    private final IMap<Integer, BookInfo> metadataMap;

    public HazelcastMetadataRepository(HazelcastInstance hazelcastInstance, MetadataReader parser) {
        this.parser = parser;
        this.metadataMap = hazelcastInstance.getMap("bookMetadata");
        this.datalake = hazelcastInstance.getMap("datalake");
    }

    @Override
    public void saveMetadata(int bookId, String header) {
        BookInfo metadata = parser.parseFromHeader(header);
        metadataMap.put(bookId, metadata);
        datalake.remove(bookId);
    }
}
