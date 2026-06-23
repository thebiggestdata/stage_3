package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexGeneration;

public final class HazelcastMetadataStore implements MetadataStore {

    private final HazelcastInstance hazelcast;

    public HazelcastMetadataStore(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public void save(IndexGeneration generation, int bookId, BookMetadata bookMetadata) {
        metadata(generation).put(bookId, bookMetadata);
    }

    @Override
    public void clear(IndexGeneration generation) {
        metadata(generation).clear();
    }

    private IMap<Integer, BookMetadata> metadata(IndexGeneration generation) {
        return hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.BOOK_METADATA,
                generation.value()
        ));
    }
}
