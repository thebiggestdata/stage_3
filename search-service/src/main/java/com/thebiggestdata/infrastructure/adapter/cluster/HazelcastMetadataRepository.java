package com.thebiggestdata.infrastructure.adapter.cluster;

import com.thebiggestdata.domain.gateway.MetadataRepository;
import com.thebiggestdata.domain.entity.BookInfo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Map;
import java.util.Set;

public class HazelcastMetadataRepository implements MetadataRepository {
	private final IMap<Integer, BookInfo> metadataMap;

	public HazelcastMetadataRepository(HazelcastInstance hazelcastInstance) {
		this.metadataMap = hazelcastInstance.getMap("bookMetadata");
	}

	@Override
	public Map<Integer, BookInfo> getMetadata(Set<Integer> bookIds) {
		return metadataMap.getAll(bookIds);
	}
}