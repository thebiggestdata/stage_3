package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.infrastructure.ports.IndexStore;
import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.IndexGeneration;
import com.thebiggestdata.search.model.SearchCriteria;
import com.thebiggestdata.search.model.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FindBooksUseCaseTest {

    @Test
    void usesOneGenerationForIndexAndMetadataAndAppliesFilters() {
        IndexGeneration generation = new IndexGeneration("stable-snapshot");
        IndexStore index = (receivedGeneration, term) -> {
            assertEquals(generation, receivedGeneration);
            return Map.of(1, 4, 2, 2);
        };
        ContentSearchEngine engine = new ContentSearchEngine(index, query -> List.of("clean"), Runnable::run);
        FindBooksUseCase useCase = new FindBooksUseCase(
                () -> generation,
                engine,
                (receivedGeneration, bookIds) -> {
                    assertEquals(generation, receivedGeneration);
                    return Map.of(
                            1, new BookMetadata("One", "Ada", "English", 2020),
                            2, new BookMetadata("Two", "Grace", "English", 2021)
                    );
                },
                new SearchResultAssembler(),
                results -> results.stream()
                        .sorted(java.util.Comparator.comparingInt(SearchResult::frequency).reversed())
                        .toList()
        );

        List<SearchResult> results = useCase.execute(
                new SearchCriteria("clean", "Ada", "English", null)
        );

        assertEquals(List.of(new SearchResult(1, "One", "Ada", "English", 2020, 4)), results);
    }
}
