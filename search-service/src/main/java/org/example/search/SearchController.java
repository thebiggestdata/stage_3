package org.example.search;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.shared.Constants;
import org.example.shared.model.BookMetadata;
import org.example.shared.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for the Search Service.
 *
 * Endpoints:
 *   GET /api/health           – liveness probe
 *   GET /api/search?q=&lt;terms&gt; – search for space-separated terms
 *   GET /api/index/size       – number of indexed terms (diagnostic)
 */
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final MultiMap<String, String>   invertedIndex;
    private final IMap<String, BookMetadata> metadataMap;

    public SearchController(HazelcastInstance hazelcast) {
        this.invertedIndex = hazelcast.getMultiMap(Constants.INVERTED_INDEX_MAP);
        this.metadataMap   = hazelcast.getMap(Constants.BOOK_METADATA_MAP);
    }

    public void register(Javalin app) {
        app.get("/api/health",     this::health);
        app.get("/api/search",     this::search);
        app.get("/api/index/size", this::indexSize);
    }

    // -------------------------------------------------------------------------

    private void health(Context ctx) {
        ctx.json(Map.of("status", "UP", "service", "search"));
    }

    private void indexSize(Context ctx) {
        ctx.json(Map.of("terms", invertedIndex.keySet().size(),
                        "books", metadataMap.size()));
    }

    /**
     * Searches for all query terms (AND semantics: a book must match all terms
     * to appear in the results) and ranks by the number of matched query terms.
     */
    private void search(Context ctx) {
        String query = ctx.queryParam("q");
        if (query == null || query.isBlank()) {
            ctx.status(400).json(Map.of("error", "'q' parameter is required"));
            return;
        }

        String[] terms = query.toLowerCase().trim().split("\\s+");
        log.info("Search query: {} (terms: {})", query, terms.length);

        // For each term collect matching book IDs, then tally relevance scores
        Map<String, Integer> bookScore = new HashMap<>();
        List<String> matchedTermsList  = new ArrayList<>();

        for (String term : terms) {
            Collection<String> bookIds = invertedIndex.get(term);
            if (bookIds != null && !bookIds.isEmpty()) {
                matchedTermsList.add(term);
                for (String bookId : bookIds) {
                    bookScore.merge(bookId, 1, Integer::sum);
                }
            }
        }

        // Build results sorted by descending relevance score
        List<SearchResult> results = bookScore.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> {
                    String bookId = e.getKey();
                    int score     = e.getValue();
                    BookMetadata meta = metadataMap.getOrDefault(bookId,
                            new BookMetadata(bookId, "Unknown", "Unknown",
                                             "Unknown", "Unknown", ""));
                    return new SearchResult(bookId, meta.getTitle(), meta.getAuthor(),
                                            matchedTermsList, score);
                })
                .collect(Collectors.toList());

        ctx.json(Map.of(
                "query",   query,
                "total",   results.size(),
                "results", results
        ));
    }
}
