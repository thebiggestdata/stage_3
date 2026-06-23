package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.IndexGeneration;

import java.util.Map;
import java.util.Set;

public interface MetadataStore {

	Map<Integer, BookMetadata> findAll(IndexGeneration generation, Set<Integer> bookIds);
}
