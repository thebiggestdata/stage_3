package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.search.infrastructure.ports.MetadataStore;
import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.IndexGeneration;

import java.util.Map;
import java.util.Set;

public final class HazelcastMetadataStore implements MetadataStore {

    private final HazelcastInstance hazelcast;

    public HazelcastMetadataStore(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<Integer, BookMetadata> findAll(IndexGeneration generation, Set<Integer> bookIds) {
        IMap<Integer, BookMetadata> metadata = hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.BOOK_METADATA,
                generation.value()
        ));
        return metadata.getAll(bookIds);
    }
}
