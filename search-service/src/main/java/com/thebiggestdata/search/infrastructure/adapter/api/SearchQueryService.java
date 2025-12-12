package com.thebiggestdata.search.infrastructure.adapter.api;

import com.thebiggestdata.search.infrastructure.port.InvertedIndexReader;
import com.thebiggestdata.search.infrastructure.port.SearchQueryProvider;
import com.thebiggestdata.search.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class SearchQueryService implements SearchQueryProvider {
    private static final Logger logger = LoggerFactory.getLogger(SearchQueryService.class);
    private final InvertedIndexReader indexReader;

    public SearchQueryService(InvertedIndexReader indexReader) {
        this.indexReader = indexReader;
    }

    @Override
    public Map<String, Object> search(String query) {
        logger.info("search() - Executing search for query: '{}'", query);
        try {
            SearchResult result = indexReader.search(query);
            return Map.of(
                    "query", result.query(),
                    "total_results", result.bookIds().size(),
                    "terms_matched", result.termsMatched(),
                    "book_ids", result.bookIds()
            );
        } catch (Exception e) {
            logger.error("search() - Error executing search: {}", e.getMessage(), e);
            return Map.of(
                    "error", e.getMessage(),
                    "query", query
            );
        }
    }
}