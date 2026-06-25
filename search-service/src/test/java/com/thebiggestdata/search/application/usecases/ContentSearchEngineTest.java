package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.infrastructure.ports.IndexStore;
import com.thebiggestdata.search.model.IndexGeneration;
import com.thebiggestdata.search.model.SearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentSearchEngineTest {

    private static final IndexGeneration GENERATION = new IndexGeneration("v1");

    private final IndexStore index = (generation, term) -> switch (term) {
        case "clean" -> Map.of(1, 2, 2, 1);
        case "code" -> Map.of(1, 1, 3, 5);
        default -> Map.of();
    };
    private final ContentSearchEngine engine = new ContentSearchEngine(
            index,
            query -> java.util.Arrays.asList(query.toLowerCase().split("\\s+")),
            Runnable::run
    );

    @Test
    void allTermsReturnsOnlyTheIntersectionAndAddsFrequencies() {
        SearchCriteria criteria = new SearchCriteria(
                "clean code", null, null, null, SearchCriteria.SearchMode.ALL_TERMS
        );

        assertEquals(Map.of(1, 3), engine.find(GENERATION, criteria));
    }

    @Test
    void anyTermReturnsTheUnionAndAddsFrequencies() {
        SearchCriteria criteria = new SearchCriteria(
                "clean code", null, null, null, SearchCriteria.SearchMode.ANY_TERM
        );

        assertEquals(Map.of(1, 3, 2, 1, 3, 5), engine.find(GENERATION, criteria));
    }

    @Test
    void repeatedQueryTermsAreEvaluatedOnce() {
        SearchCriteria criteria = new SearchCriteria(
                "clean clean", null, null, null, SearchCriteria.SearchMode.ALL_TERMS
        );

        assertEquals(Map.of(1, 2, 2, 1), engine.find(GENERATION, criteria));
    }
}
