package com.thebiggestdata.domain.gateway;

import java.util.Collection;
import java.util.Set;

public interface IndexStore {
    void addEntry(String term, String documentId, Long frequency);
    void pushEntries();
    Set<String> getDocuments(String term);
    void clear();
    Collection<Integer> retrieveIndexingRegistry();
    void saveTokens(Integer tokenCount);
}
