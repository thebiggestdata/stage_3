package com.thebiggestdata.infrastructure.adapter.ranker;

import com.thebiggestdata.domain.gateway.RankingStrategy;
import com.thebiggestdata.domain.entity.SearchHit;

import java.util.List;

public class RankByFrequency implements RankingStrategy {
	@Override
	public void sort(List<SearchHit> results){
		results.sort((a, b) -> Integer.compare(b.frequency(), a.frequency()));
	}
}