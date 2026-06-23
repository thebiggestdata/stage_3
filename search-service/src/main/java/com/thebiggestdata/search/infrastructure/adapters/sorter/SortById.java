package com.thebiggestdata.search.infrastructure.adapters.sorter;

import com.thebiggestdata.search.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.search.model.SearchResult;

import java.util.Comparator;
import java.util.List;

public final class SortById implements SortingStrategy {

    @Override
    public List<SearchResult> sort(List<SearchResult> results) {
        return results.stream().sorted(Comparator.comparingInt(SearchResult::id)).toList();
    }
}
