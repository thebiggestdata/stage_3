package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.BookInfo;
import java.util.Map;
import java.util.Set;

public interface MetadataRepository {
	Map<Integer, BookInfo> getMetadata(Set<Integer> bookIds);
}