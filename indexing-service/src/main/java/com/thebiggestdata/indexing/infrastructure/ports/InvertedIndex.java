package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexGeneration;

import java.util.List;

public interface InvertedIndex {

    void addAll(IndexGeneration generation, List<IndexedTerm> terms);

    void clear(IndexGeneration generation);
}
