package com.thebiggestdata.search.infrastructure.adapter.sorter;

import com.thebiggestdata.search.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.search.SearchResult;

import java.util.List;

public class SortByFrequency implements SortingStrategy {
	@Override
	public void sort(List<SearchResult> results){
		results.sort((a, b) -> Integer.compare(b.frequency(), a.frequency()));
	}
}