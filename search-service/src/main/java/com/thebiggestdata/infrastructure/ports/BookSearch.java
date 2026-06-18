package com.thebiggestdata.infrastructure.ports;

import com.thebiggestdata.model.SearchCriteria;
import com.thebiggestdata.model.SearchResult;
import java.util.List;

public interface BookSearch {
	List<SearchResult> execute(SearchCriteria criteria);
}