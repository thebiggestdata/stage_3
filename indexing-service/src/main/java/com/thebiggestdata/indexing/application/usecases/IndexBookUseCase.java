package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataExtractor;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.BookContent;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingClaim;
import com.thebiggestdata.indexing.model.IndexingResult;
import com.thebiggestdata.indexing.model.IndexGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class IndexBookUseCase {

    private static final Logger log = LoggerFactory.getLogger(IndexBookUseCase.class);

    private final BookContentStore books;
    private final InvertedIndex index;
    private final IndexingTracker tracker;
    private final IndexGenerationStore generations;
    private final MetadataStore metadata;
    private final TokenMetrics tokenMetrics;
    private final TermFrequencyAnalyzer analyzer;
    private final MetadataExtractor metadataExtractor;

    public IndexBookUseCase(
            BookContentStore books,
            InvertedIndex index,
            IndexingTracker tracker,
            IndexGenerationStore generations,
            MetadataStore metadata,
            TokenMetrics tokenMetrics,
            TermFrequencyAnalyzer analyzer,
            MetadataExtractor metadataExtractor
    ) {
        this.books = books;
        this.index = index;
        this.tracker = tracker;
        this.generations = generations;
        this.metadata = metadata;
        this.tokenMetrics = tokenMetrics;
        this.analyzer = analyzer;
        this.metadataExtractor = metadataExtractor;
    }

    public IndexingResult execute(int bookId) {
        return execute(bookId, generations.active(), true);
    }

    public IndexingResult execute(
            int bookId,
            IndexGeneration generation,
            boolean removeFromLiveDatalake
    ) {
        IndexingClaim claim = tracker.claim(generation, bookId);
        if (claim == IndexingClaim.ALREADY_INDEXED) {
            if (removeFromLiveDatalake) {
                removeIndexedBook(bookId);
            }
            return IndexingResult.alreadyIndexed(bookId);
        }
        if (claim == IndexingClaim.IN_PROGRESS) {
            return IndexingResult.inProgress(bookId);
        }

        try {
            BookContent content = books.get(bookId);
            TermFrequencyAnalyzer.Analysis analysis = analyzer.analyze(content.body());
            List<IndexedTerm> terms = analysis.frequencies().entrySet().stream()
                    .map(entry -> new IndexedTerm(entry.getKey(), bookId, entry.getValue()))
                    .toList();

            index.addAll(generation, terms);
            metadata.save(generation, bookId, metadataExtractor.extract(content.header()));
            tokenMetrics.record(generation, bookId, analysis.totalTokens());
            tracker.complete(generation, bookId);
            if (removeFromLiveDatalake) {
                removeIndexedBook(bookId);
            }

            return IndexingResult.indexed(bookId, terms.size(), analysis.totalTokens());
        } catch (RuntimeException e) {
            tracker.release(generation, bookId);
            log.error("Could not index book {}", bookId, e);
            return IndexingResult.failed(bookId, e.getMessage());
        }
    }

    private void removeIndexedBook(int bookId) {
        try {
            books.remove(bookId);
        } catch (RuntimeException e) {
            log.warn("Book {} was indexed but could not be removed from the live datalake", bookId, e);
        }
    }
}
