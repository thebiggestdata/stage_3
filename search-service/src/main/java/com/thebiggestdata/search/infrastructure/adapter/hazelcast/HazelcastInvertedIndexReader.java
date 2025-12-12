// HazelcastInvertedIndexReader.java
package com.thebiggestdata.search.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.search.infrastructure.port.InvertedIndexReader;
import com.thebiggestdata.search.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HazelcastInvertedIndexReader implements InvertedIndexReader {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastInvertedIndexReader.class);
    private final HazelcastInstance hazelcast;

    public HazelcastInvertedIndexReader(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public SearchResult search(String query) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");

        // Tokenize query (simple: lowercase and split)
        String[] terms = query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

        logger.info("Searching for terms: {}", Arrays.toString(terms));

        // Get book IDs for each term
        Map<String, Collection<Integer>> termResults = new HashMap<>();
        for (String term : terms) {
            if (!term.isEmpty() && term.length() > 2) {
                Collection<Integer> bookIds = invertedIndex.get(term);
                termResults.put(term, bookIds);
                logger.debug("Term '{}' found in {} books", term, bookIds.size());
            }
        }

        // Find books that contain ANY of the terms (OR logic)
        Set<Integer> allBookIds = termResults.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // Count occurrences for ranking (books with more matching terms rank higher)
        Map<Integer, Integer> bookScores = new HashMap<>();
        for (Integer bookId : allBookIds) {
            int score = 0;
            for (Collection<Integer> books : termResults.values()) {
                if (books.contains(bookId)) {
                    score++;
                }
            }
            bookScores.put(bookId, score);
        }

        // Sort by score (descending)
        List<Integer> rankedBookIds = bookScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        logger.info("Found {} books matching query '{}'", rankedBookIds.size(), query);

        return new SearchResult(query, rankedBookIds, termResults.size());
    }

    @Override
    public Collection<Integer> getDocuments(String term) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap("inverted-index");
        return invertedIndex.get(term.toLowerCase());
    }
}