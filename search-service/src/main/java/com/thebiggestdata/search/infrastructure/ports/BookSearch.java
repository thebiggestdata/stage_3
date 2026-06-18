package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.SearchCriteria;
import com.thebiggestdata.search.model.SearchResult;
import java.util.List;

public interface BookSearch {
	List<SearchResult> execute(SearchCriteria criteria);
}