package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.SearchResult;

import java.util.List;

public interface SortingStrategy {
	List<SearchResult> sort(List<SearchResult> results);
}
