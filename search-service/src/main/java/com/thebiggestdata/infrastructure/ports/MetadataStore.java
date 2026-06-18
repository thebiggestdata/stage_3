package com.thebiggestdata.infrastructure.ports;

import com.thebiggestdata.model.BookMetadata;
import java.util.Map;
import java.util.Set;

public interface MetadataStore {
	Map<Integer, BookMetadata> getMetadata(Set<Integer> bookIds);
}