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
    private final String localNodeId;

    public IndexBookUseCase(
            BookContentStore books,
            InvertedIndex index,
            IndexingTracker tracker,
            IndexGenerationStore generations,
            MetadataStore metadata,
            TokenMetrics tokenMetrics,
            TermFrequencyAnalyzer analyzer,
            MetadataExtractor metadataExtractor,
            String localNodeId
    ) {
        this.books = books;
        this.index = index;
        this.tracker = tracker;
        this.generations = generations;
        this.metadata = metadata;
        this.tokenMetrics = tokenMetrics;
        this.analyzer = analyzer;
        this.metadataExtractor = metadataExtractor;
        this.localNodeId = localNodeId;
    }

    public IndexingResult execute(int bookId) {
        return execute(bookId, "unknown");
    }

    public IndexingResult execute(int bookId, String sourceNodeId) {
        return execute(bookId, generations.active(), true, sourceNodeId);
    }

    public IndexingResult execute(
            int bookId,
            IndexGeneration generation,
            boolean removeFromLiveDatalake
    ) {
        return execute(bookId, generation, removeFromLiveDatalake, "unknown");
    }

    public IndexingResult execute(
            int bookId,
            IndexGeneration generation,
            boolean removeFromLiveDatalake,
            String sourceNodeId
    ) {
        String normalizedSourceNodeId = sourceNodeId == null || sourceNodeId.isBlank()
                ? "unknown"
                : sourceNodeId;
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
            log.info(
                    "INDEXING_STARTED bookId={} ingestedBy={} indexedBy={} generation={}",
                    bookId,
                    normalizedSourceNodeId,
                    localNodeId,
                    generation.value()
            );
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

            log.info(
                    "INDEXED bookId={} ingestedBy={} indexedBy={} generation={} terms={} tokens={}",
                    bookId,
                    normalizedSourceNodeId,
                    localNodeId,
                    generation.value(),
                    terms.size(),
                    analysis.totalTokens()
            );
            return IndexingResult.indexed(bookId, terms.size(), analysis.totalTokens());
        } catch (RuntimeException e) {
            tracker.release(generation, bookId);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error(
                    "INDEXING_FAILED bookId={} ingestedBy={} indexedBy={} generation={} reason={}",
                    bookId,
                    normalizedSourceNodeId,
                    localNodeId,
                    generation.value(),
                    reason,
                    e
            );
            return IndexingResult.failed(bookId, reason);
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
