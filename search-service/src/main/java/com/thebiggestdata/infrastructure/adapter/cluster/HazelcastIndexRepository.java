package com.thebiggestdata.infrastructure.adapter.cluster;

import com.thebiggestdata.domain.gateway.IndexRepository;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HazelcastIndexRepository implements IndexRepository {
	private static final Logger log = LoggerFactory.getLogger(HazelcastIndexRepository.class);
	private final IMap<String, Set<String>> invertedIndex;

	public HazelcastIndexRepository(HazelcastInstance hazelcastInstance) {
		this.invertedIndex = hazelcastInstance.getMap("inverted-index");
		log.info("Connected to Hazelcast inverted index");
	}

	@Override
	public Set<String> getDocuments(String term) {
		Collection<String> docs = invertedIndex.get(term);

		if (docs == null) {
			return Collections.emptySet();
		}

		return new HashSet<>(docs);
	}
}