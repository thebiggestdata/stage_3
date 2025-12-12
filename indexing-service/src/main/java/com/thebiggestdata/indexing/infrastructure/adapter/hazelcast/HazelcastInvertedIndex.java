package com.thebiggestdata.indexing.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.indexing.infrastructure.port.InvertedIndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class HazelcastInvertedIndex implements InvertedIndexWriter {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastInvertedIndex.class);
    private static final String INDEX_MAP = "inverted-index";
    private static final String INDEXED_BOOKS_MAP = "indexed-books";

    private final HazelcastInstance hazelcast;

    public HazelcastInvertedIndex(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public void indexBook(int bookId, List<String> tokens) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap(INDEX_MAP);
        IMap<Integer, Boolean> indexedBooks = hazelcast.getMap(INDEXED_BOOKS_MAP);
        logger.info("Indexing book {} with {} unique tokens", bookId, tokens.size());
        int indexed = 0;
        for (String token : tokens) {
            invertedIndex.put(token, bookId);
            indexed++;
        }
        indexedBooks.put(bookId, true);

        logger.info("Book {} indexed successfully: {} tokens added to inverted index", bookId, indexed);
    }

    @Override
    public boolean isIndexed(int bookId) {
        IMap<Integer, Boolean> indexedBooks = hazelcast.getMap(INDEXED_BOOKS_MAP);
        return indexedBooks.containsKey(bookId);
    }

    @Override
    public Collection<Integer> getDocuments(String word) {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap(INDEX_MAP);
        return invertedIndex.get(word.toLowerCase());
    }

    @Override
    public int getTotalIndexedBooks() {
        IMap<Integer, Boolean> indexedBooks = hazelcast.getMap(INDEXED_BOOKS_MAP);
        return indexedBooks.size();
    }

    @Override
    public int getTotalTerms() {
        MultiMap<String, Integer> invertedIndex = hazelcast.getMultiMap(INDEX_MAP);
        return invertedIndex.keySet().size();
    }
}