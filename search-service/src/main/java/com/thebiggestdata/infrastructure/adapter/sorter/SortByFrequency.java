package com.thebiggestdata.infrastructure.adapter.sorter;

import com.thebiggestdata.domain.gateway.SortingStrategy;
import com.thebiggestdata.domain.entity.SearchResult;

import java.util.List;

public class SortByFrequency implements SortingStrategy {
	@Override
	public void sort(List<SearchResult> results){
		results.sort((a, b) -> Integer.compare(b.frequency(), a.frequency()));
	}
}