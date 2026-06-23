package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.search.infrastructure.ports.IndexStore;
import com.thebiggestdata.search.model.IndexGeneration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HazelcastIndexStore implements IndexStore {

    private final HazelcastInstance hazelcast;

    public HazelcastIndexStore(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Map<Integer, Integer> find(IndexGeneration generation, String term) {
        IMap<String, Set<String>> index = hazelcast.getMap(HazelcastNames.generated(
                HazelcastNames.INVERTED_INDEX,
                generation.value()
        ));
        Set<String> encodedPostings = index.get(term);
        if (encodedPostings == null || encodedPostings.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Integer> postings = new HashMap<>();
        for (String encodedPosting : encodedPostings) {
            Posting posting = decode(encodedPosting);
            postings.merge(posting.bookId(), posting.frequency(), Math::max);
        }
        return Map.copyOf(postings);
    }

    private Posting decode(String encodedPosting) {
        try {
            int separator = encodedPosting.indexOf(':');
            if (separator < 1 || separator == encodedPosting.length() - 1) {
                throw new IllegalArgumentException("Missing posting separator");
            }
            return new Posting(
                    Integer.parseInt(encodedPosting.substring(0, separator)),
                    Math.toIntExact(Long.parseLong(encodedPosting.substring(separator + 1)))
            );
        } catch (RuntimeException e) {
            throw new HazelcastAdapterException("Invalid index posting: " + encodedPosting, e);
        }
    }

    private record Posting(int bookId, int frequency) {}
}
