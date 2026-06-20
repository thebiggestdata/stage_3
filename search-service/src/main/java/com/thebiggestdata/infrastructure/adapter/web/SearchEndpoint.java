package com.thebiggestdata.infrastructure.adapter.web;

import com.thebiggestdata.domain.gateway.BookSearchEngine;
import com.thebiggestdata.domain.entity.SearchQuery;
import com.thebiggestdata.domain.entity.SearchHit;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SearchEndpoint {
	private static final Logger log = Logger.getLogger(SearchEndpoint.class.getName());

	private final BookSearchEngine bookSearch;
	private final SearchQueryMapper mapper;
	private final SearchHitPresenter presenter;

	public SearchEndpoint(BookSearchEngine bookSearch) {
		this.bookSearch = bookSearch;
		this.mapper = new SearchQueryMapper();
		this.presenter = new SearchHitPresenter();
	}

	public void search(Context ctx) {
		try {
			SearchQuery criteria = mapper.map(ctx);
			log.info("Executing search for: " + criteria.query());

			List<SearchHit> results = bookSearch.execute(criteria);

			Map<String, Object> jsonResponse = presenter.formatSuccess(criteria, results);

			ctx.json(jsonResponse);
		} catch (IllegalArgumentException e) {
			ctx.status(400).json(presenter.formatError(e.getMessage()));
		} catch (Exception e) {
			log.severe("Error: " + e.getMessage());
			ctx.status(500).json(presenter.formatError(e.getMessage()));
		}
	}

	public void health(Context ctx) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "healthy");
		response.put("service", "execute");
		ctx.json(response);
	}
}
