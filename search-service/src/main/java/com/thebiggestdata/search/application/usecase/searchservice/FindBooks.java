package com.thebiggestdata.search.application.usecase.searchservice;

import com.thebiggestdata.model.SearchCriteria;
import com.thebiggestdata.model.BookMetadata;
import com.thebiggestdata.model.SearchResult;
import com.thebiggestdata.infrastructure.ports.BookSearch;
import com.thebiggestdata.infrastructure.ports.MetadataStore;
import com.thebiggestdata.infrastructure.ports.SortingStrategy;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FindBooks implements BookSearch {
	private static final Logger log = Logger.getLogger(FindBooks.class.getName());

	private final ContentSearchEngine searchEngine;
	private final MetadataStore metadataStore;
	private final SortingStrategy sortingStrategy;

	public FindBooks(ContentSearchEngine searchEngine, MetadataStore metadataStore, SortingStrategy sortingStrategy) {
		this.searchEngine = searchEngine;
		this.metadataStore = metadataStore;
		this.sortingStrategy = sortingStrategy;
	}

	@Override
	public List<SearchResult> execute(SearchCriteria criteria) {
		String query = criteria.query();
		String author = criteria.author();
		String language = criteria.language();
		Integer year = criteria.year();

		long startTime = System.currentTimeMillis();

		Map<String, Integer> contentMatches = searchEngine.findDocumentFrequencies(query);

		if (contentMatches.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Integer, BookMetadata> metadata = fetchMetadata(contentMatches.keySet());

		List<SearchResult> results = buildAndFilterResults(contentMatches, metadata, author, language, year);

		sortingStrategy.sort(results);

		long duration = System.currentTimeMillis() - startTime;
		log.info(String.format("Search finished: %d results in %dms", results.size(), duration));

		return results;
	}

	private Map<Integer, BookMetadata> fetchMetadata(Set<String> docIdsStr) {
		Set<Integer> docIds = docIdsStr.stream()
				.map(Integer::parseInt)
				.collect(Collectors.toSet());
		return metadataStore.getMetadata(docIds);
	}

	private List<SearchResult> buildAndFilterResults(Map<String, Integer> matches,Map<Integer, BookMetadata> metadataMap,
	                                                 String author, String lang, Integer year) {
		List<SearchResult> results = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : matches.entrySet()) {
			int docId = Integer.parseInt(entry.getKey());
			BookMetadata meta = metadataMap.get(docId);

			if (meta != null && meta.matches(author, lang, year)) {
				results.add(mapToResult(docId, meta, entry.getValue()));
			}
		}
		return results;
	}

	private SearchResult mapToResult(int id, BookMetadata meta, int frequency) {
		return new SearchResult(
				id,
				meta.title(),
				meta.author(),
				meta.language(),
				meta.year() != null ? meta.year() : 0,
				frequency
		);
	}
}