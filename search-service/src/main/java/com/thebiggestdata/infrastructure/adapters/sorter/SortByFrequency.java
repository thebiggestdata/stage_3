package com.thebiggestdata.infrastructure.adapters.sorter;

import com.thebiggestdata.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.model.SearchResult;

import java.util.List;

public class SortByFrequency implements SortingStrategy {
	@Override
	public void sort(List<SearchResult> results){
		results.sort((a, b) -> Integer.compare(b.frequency(), a.frequency()));
	}
}