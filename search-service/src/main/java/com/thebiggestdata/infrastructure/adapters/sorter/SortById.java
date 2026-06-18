package com.thebiggestdata.infrastructure.adapters.sorter;

import com.thebiggestdata.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.model.SearchResult;

import java.util.List;

public class SortById implements SortingStrategy {
	@Override
	public void sort(List<SearchResult> results){
		results.sort((a, b) -> Integer.compare(a.id(), b.id()));
	}
}