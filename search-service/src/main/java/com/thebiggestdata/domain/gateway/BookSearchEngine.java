package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.SearchQuery;
import com.thebiggestdata.domain.entity.SearchHit;
import java.util.List;

public interface BookSearchEngine {
	List<SearchHit> execute(SearchQuery criteria);
}