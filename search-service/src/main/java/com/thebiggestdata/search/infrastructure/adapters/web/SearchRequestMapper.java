package com.thebiggestdata.search.infrastructure.adapters.web;

import com.thebiggestdata.search.model.SearchCriteria;
import io.javalin.http.Context;

public final class SearchRequestMapper {

	public SearchCriteria map(Context ctx) {
		String query = ctx.queryParam("q");

		if (query == null || query.trim().isEmpty()) {
			throw new IllegalArgumentException("Query parameter 'q' is required");
		}

		String author = ctx.queryParam("author");
		String language = ctx.queryParam("language");
		Integer year = parseYear(ctx.queryParam("year"));
		SearchCriteria.SearchMode mode = parseMode(ctx.queryParam("mode"));

		return new SearchCriteria(query, author, language, year, mode);
	}

	private Integer parseYear(String yearStr) {
		if (yearStr == null || yearStr.isBlank()) return null;
		try {
			return Integer.parseInt(yearStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid year format");
		}
	}

	private SearchCriteria.SearchMode parseMode(String mode) {
		if (mode == null || mode.isBlank() || mode.equalsIgnoreCase("all")) {
			return SearchCriteria.SearchMode.ALL_TERMS;
		}
		if (mode.equalsIgnoreCase("any")) {
			return SearchCriteria.SearchMode.ANY_TERM;
		}
		throw new IllegalArgumentException("Invalid search mode; expected 'all' or 'any'");
	}
}
