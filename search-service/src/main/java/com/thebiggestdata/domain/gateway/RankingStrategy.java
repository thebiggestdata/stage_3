package com.thebiggestdata.domain.gateway;

import com.thebiggestdata.domain.entity.SearchHit;
import java.util.List;

public interface RankingStrategy {
	void sort(List<SearchHit> results);
}
