package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexedTerm;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface InvertedIndexPort {
    boolean markAsIndexed(int bookId);
    void addEntries(List<IndexedTerm> entries);
    void pushEntries();
    Set<String> getDocuments(String term);
    Collection<Integer> retrieveIndexingRegistry();
    void saveTokens(int tokenCount);
}
