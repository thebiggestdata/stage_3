package com.thebiggestdata.infrastructure.adapter.sorter;

import com.thebiggestdata.domain.gateway.RankingStrategy;
import com.thebiggestdata.domain.entity.SearchHit;

import java.util.List;

public class SortById implements RankingStrategy {
	@Override
	public void sort(List<SearchHit> results){
		results.sort((a, b) -> Integer.compare(a.id(), b.id()));
	}
}