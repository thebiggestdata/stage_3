package com.thebiggestdata.usecase;

import com.thebiggestdata.domain.entity.BookInfo;
import com.thebiggestdata.domain.entity.SearchQuery;
import com.thebiggestdata.domain.entity.SearchHit;
import com.thebiggestdata.domain.gateway.BookSearchEngine;
import com.thebiggestdata.domain.gateway.MetadataRepository;
import com.thebiggestdata.domain.gateway.RankingStrategy;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FindBooks implements BookSearchEngine {
	private static final Logger log = Logger.getLogger(FindBooks.class.getName());

	private final ContentSearchEngine searchEngine;
	private final MetadataRepository metadataStore;
	private final RankingStrategy sortingStrategy;

	public FindBooks(ContentSearchEngine searchEngine, MetadataRepository metadataStore, RankingStrategy sortingStrategy) {
		this.searchEngine = searchEngine;
		this.metadataStore = metadataStore;
		this.sortingStrategy = sortingStrategy;
	}

	@Override
	public List<SearchHit> execute(SearchQuery criteria) {
		String query = criteria.query();
		String author = criteria.author();
		String language = criteria.language();
		Integer year = criteria.year();

		long startTime = System.currentTimeMillis();

		Map<String, Integer> contentMatches = searchEngine.findDocumentFrequencies(query);

		if (contentMatches.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Integer, BookInfo> metadata = fetchMetadata(contentMatches.keySet());

		List<SearchHit> results = buildAndFilterResults(contentMatches, metadata, author, language, year);

		sortingStrategy.sort(results);

		long duration = System.currentTimeMillis() - startTime;
		log.info(String.format("Search finished: %d results in %dms", results.size(), duration));

		return results;
	}

	private Map<Integer, BookInfo> fetchMetadata(Set<String> docIdsStr) {
		Set<Integer> docIds = docIdsStr.stream()
				.map(Integer::parseInt)
				.collect(Collectors.toSet());
		return metadataStore.getMetadata(docIds);
	}

	private List<SearchHit> buildAndFilterResults(Map<String, Integer> matches, Map<Integer, BookInfo> metadataMap,
	                                              String author, String lang, Integer year) {
		List<SearchHit> results = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : matches.entrySet()) {
			int docId = Integer.parseInt(entry.getKey());
			BookInfo meta = metadataMap.get(docId);

			if (meta != null && meta.matches(author, lang, year)) {
				results.add(mapToResult(docId, meta, entry.getValue()));
			}
		}
		return results;
	}

	private SearchHit mapToResult(int id, BookInfo meta, int frequency) {
		return new SearchHit(
				id,
				meta.title(),
				meta.author(),
				meta.language(),
				meta.year() != null ? meta.year() : 0,
				frequency
		);
	}
}