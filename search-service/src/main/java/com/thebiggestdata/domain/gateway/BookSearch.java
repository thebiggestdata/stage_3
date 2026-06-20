package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.SearchCriteria;
import com.thebiggestdata.domain.entity.SearchResult;
import java.util.List;

public interface BookSearch {
	List<SearchResult> execute(SearchCriteria criteria);
}