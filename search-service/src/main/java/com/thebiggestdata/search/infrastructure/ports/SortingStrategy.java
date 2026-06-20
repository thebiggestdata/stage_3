package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.SearchResult;
import java.util.List;

public interface SortingStrategy {
	void sort(List<SearchResult> results);
}
