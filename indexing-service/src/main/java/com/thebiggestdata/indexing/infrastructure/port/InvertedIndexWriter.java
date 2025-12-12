package com.thebiggestdata.indexing.infrastructure.port;

import java.util.Collection;
import java.util.List;

public interface InvertedIndexWriter {
    void indexBook(int bookId, List<String> tokens);
    boolean isIndexed(int bookId);
    Collection<Integer> getDocuments(String word);
    int getTotalIndexedBooks();
    int getTotalTerms();
}