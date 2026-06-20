package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.thebiggestdata.domain.gateway.MetadataStore;
import com.thebiggestdata.domain.entity.BookMetadata;
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