package com.thebiggestdata.search.infrastructure.adapters.sorter;

import com.thebiggestdata.search.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.search.model.SearchResult;

import java.util.List;

public class SortById implements SortingStrategy {
	@Override
	public void sort(List<SearchResult> results){
		results.sort((a, b) -> Integer.compare(a.id(), b.id()));
	}
}