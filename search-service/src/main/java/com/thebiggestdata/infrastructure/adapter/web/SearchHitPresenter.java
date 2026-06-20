package com.thebiggestdata.infrastructure.adapter.web;

import com.thebiggestdata.domain.entity.SearchQuery;
import com.thebiggestdata.domain.entity.SearchHit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchHitPresenter {

	public Map<String, Object> formatSuccess(SearchQuery criteria, List<SearchHit> results) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "success");
		response.put("query", criteria.query());
		response.put("filters", extractFilters(criteria));
		response.put("count", results.size());

		List<Map<String, Object>> mappedResults = results.stream()
				.map(this::mapSingleResult)
				.collect(Collectors.toList());

		response.put("results", mappedResults);
		return response;
	}

	public Map<String, Object> formatError(String message) {
		return Map.of("status", "error", "message", message);
	}

	private Map<String, Object> extractFilters(SearchQuery c) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (c.author() != null) filters.put("author", c.author());
		if (c.language() != null) filters.put("language", c.language());
		if (c.year() != null) filters.put("year", c.year());
		return filters;
	}

	private Map<String, Object> mapSingleResult(SearchHit r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.id());
		m.put("title", r.title());
		m.put("author", r.author());
		m.put("language", r.language());
		m.put("year", r.year());
		m.put("frequency", r.frequency());
		return m;
	}
}
