package com.thebiggestdata.infrastructure.ports;

import com.thebiggestdata.model.SearchResult;
import java.util.List;

public interface SortingStrategy {
	void sort(List<SearchResult> results);
}
