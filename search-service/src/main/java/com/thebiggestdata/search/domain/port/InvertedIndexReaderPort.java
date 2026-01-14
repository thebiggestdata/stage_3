package com.thebiggestdata.search.domain.port;

import java.util.Set;

public interface InvertedIndexReaderPort {
    Set<String> getDocumentsForTerm(String term);
}