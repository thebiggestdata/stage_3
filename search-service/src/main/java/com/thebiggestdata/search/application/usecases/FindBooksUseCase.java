package com.thebiggestdata.search.application.usecases;

import com.thebiggestdata.search.infrastructure.ports.BookSearch;
import com.thebiggestdata.search.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.search.infrastructure.ports.MetadataStore;
import com.thebiggestdata.search.infrastructure.ports.SortingStrategy;
import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.IndexGeneration;
import com.thebiggestdata.search.model.SearchCriteria;
import com.thebiggestdata.search.model.SearchResult;

import java.util.List;
import java.util.Map;

public final class FindBooksUseCase implements BookSearch {

    private final IndexGenerationStore generations;
    private final ContentSearchEngine searchEngine;
    private final MetadataStore metadataStore;
    private final SearchResultAssembler assembler;
    private final SortingStrategy sorting;

    public FindBooksUseCase(
            IndexGenerationStore generations,
            ContentSearchEngine searchEngine,
            MetadataStore metadataStore,
            SearchResultAssembler assembler,
            SortingStrategy sorting
    ) {
        this.generations = generations;
        this.searchEngine = searchEngine;
        this.metadataStore = metadataStore;
        this.assembler = assembler;
        this.sorting = sorting;
    }

    @Override
    public List<SearchResult> execute(SearchCriteria criteria) {
        validate(criteria);
        IndexGeneration generation = generations.active();
        Map<Integer, Integer> matches = searchEngine.find(generation, criteria);
        if (matches.isEmpty()) {
            return List.of();
        }

        Map<Integer, BookMetadata> metadata = metadataStore.findAll(generation, matches.keySet());
        return sorting.sort(assembler.assemble(matches, metadata, criteria));
    }

    private void validate(SearchCriteria criteria) {
        if (criteria == null || criteria.query() == null || criteria.query().isBlank()) {
            throw new IllegalArgumentException("Query cannot be blank");
        }
    }
}
