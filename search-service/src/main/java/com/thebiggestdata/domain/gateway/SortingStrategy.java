package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.SearchResult;
import java.util.List;

public interface SortingStrategy {
	void sort(List<SearchResult> results);
}
