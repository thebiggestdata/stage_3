package com.thebiggestdata.infrastructure.adapters.hazelcast;

import com.thebiggestdata.infrastructure.ports.MetadataStore;
import com.thebiggestdata.model.BookMetadata;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Map;
import java.util.Set;

public class HazelcastMetadataStore implements MetadataStore {
	private final IMap<Integer, BookMetadata> metadataMap;

	public HazelcastMetadataStore(HazelcastInstance hazelcastInstance) {
		this.metadataMap = hazelcastInstance.getMap("bookMetadata");
	}

	@Override
	public Map<Integer, BookMetadata> getMetadata(Set<Integer> bookIds) {
		return metadataMap.getAll(bookIds);
	}
}