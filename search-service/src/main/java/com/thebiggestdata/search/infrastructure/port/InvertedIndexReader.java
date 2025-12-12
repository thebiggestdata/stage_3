package com.thebiggestdata.search.infrastructure.port;

import com.thebiggestdata.search.model.SearchResult;
import java.util.Collection;

public interface InvertedIndexReader {
    SearchResult search(String query);
    Collection<Integer> getDocuments(String term);
}