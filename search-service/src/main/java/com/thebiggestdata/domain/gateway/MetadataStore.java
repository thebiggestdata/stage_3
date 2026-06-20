package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookMetadata;
import java.util.Map;
import java.util.Set;

public interface MetadataStore {
	Map<Integer, BookMetadata> getMetadata(Set<Integer> bookIds);
}