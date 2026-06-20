package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.BookMetadata;
import java.util.Map;
import java.util.Set;

public interface MetadataStore {
	Map<Integer, BookMetadata> getMetadata(Set<Integer> bookIds);
}