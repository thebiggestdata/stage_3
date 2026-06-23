package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.BookContent;
import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingClaim;
import com.thebiggestdata.indexing.model.IndexingResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexBookUseCaseTest {

    private static final IndexGeneration GENERATION = new IndexGeneration("v1");

    @Test
    void confirmsIdempotencyOnlyAfterEveryIndexWriteSucceeds() {
        List<String> operations = new ArrayList<>();
        Tracker tracker = new Tracker(IndexingClaim.ACQUIRED, operations);
        Books books = new Books(operations);
        IndexBookUseCase useCase = useCase(
                books,
                tracker,
                (generation, terms) -> operations.add("index"),
                (generation, bookId, metadata) -> operations.add("metadata"),
                (generation, bookId, count) -> operations.add("metrics")
        );

        IndexingResult result = useCase.execute(42);

        assertEquals(IndexingResult.Status.INDEXED, result.status());
        assertEquals(2, result.uniqueTermsIndexed());
        assertEquals(3, result.totalTokens());
        assertEquals(
                List.of("claim", "read", "index", "metadata", "metrics", "complete", "remove"),
                operations
        );
    }

    @Test
    void releasesClaimWhenAnIndexAdapterFails() {
        List<String> operations = new ArrayList<>();
        Tracker tracker = new Tracker(IndexingClaim.ACQUIRED, operations);
        Books books = new Books(operations);
        IndexBookUseCase useCase = useCase(
                books,
                tracker,
                (generation, terms) -> { throw new IllegalStateException("hazelcast unavailable"); },
                (generation, bookId, metadata) -> {},
                (generation, bookId, count) -> {}
        );

        IndexingResult result = useCase.execute(42);

        assertEquals(IndexingResult.Status.FAILED, result.status());
        assertTrue(operations.contains("release"));
        assertTrue(!operations.contains("complete"));
        assertTrue(!operations.contains("remove"));
    }

    @Test
    void skipsWorkForAnAlreadyIndexedBookAndCleansTheLiveBuffer() {
        List<String> operations = new ArrayList<>();
        Books books = new Books(operations);
        IndexBookUseCase useCase = useCase(
                books,
                new Tracker(IndexingClaim.ALREADY_INDEXED, operations),
                (generation, terms) -> operations.add("index"),
                (generation, bookId, metadata) -> operations.add("metadata"),
                (generation, bookId, count) -> operations.add("metrics")
        );

        IndexingResult result = useCase.execute(42);

        assertEquals(IndexingResult.Status.ALREADY_INDEXED, result.status());
        assertEquals(List.of("claim", "remove"), operations);
    }

    private IndexBookUseCase useCase(
            Books books,
            Tracker tracker,
            IndexWriter index,
            MetadataWriter metadata,
            MetricsWriter metrics
    ) {
        InvertedIndex invertedIndex = new InvertedIndex() {
            @Override public void addAll(IndexGeneration generation, List<IndexedTerm> terms) {
                index.write(generation, terms);
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        MetadataStore metadataStore = new MetadataStore() {
            @Override public void save(IndexGeneration generation, int bookId, BookMetadata value) {
                metadata.write(generation, bookId, value);
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        TokenMetrics tokenMetrics = new TokenMetrics() {
            @Override public void record(IndexGeneration generation, int bookId, int count) {
                metrics.write(generation, bookId, count);
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        return new IndexBookUseCase(
                books,
                invertedIndex,
                tracker,
                generations(),
                metadataStore,
                tokenMetrics,
                new TermFrequencyAnalyzer(text -> List.of("clean", "code", "clean")),
                header -> new BookMetadata("Title", "Author", "English", 2026)
        );
    }

    private IndexGenerationStore generations() {
        return new IndexGenerationStore() {
            @Override public IndexGeneration active() { return GENERATION; }
            @Override public void prepare(IndexGeneration generation) {}
            @Override public void activate(IndexGeneration generation) {}
        };
    }

    private static final class Books implements BookContentStore {
        private final List<String> operations;

        private Books(List<String> operations) { this.operations = operations; }
        @Override public BookContent get(int bookId) {
            operations.add("read");
            return new BookContent("header", "body");
        }
        @Override public void save(int bookId, BookContent content) {}
        @Override public void remove(int bookId) { operations.add("remove"); }
    }

    private static final class Tracker implements IndexingTracker {
        private final IndexingClaim claim;
        private final List<String> operations;

        private Tracker(IndexingClaim claim, List<String> operations) {
            this.claim = claim;
            this.operations = operations;
        }
        @Override public IndexingClaim claim(IndexGeneration generation, int bookId) {
            operations.add("claim");
            return claim;
        }
        @Override public void complete(IndexGeneration generation, int bookId) { operations.add("complete"); }
        @Override public void release(IndexGeneration generation, int bookId) { operations.add("release"); }
        @Override public void clear(IndexGeneration generation) {}
    }

    @FunctionalInterface private interface IndexWriter {
        void write(IndexGeneration generation, List<IndexedTerm> terms);
    }
    @FunctionalInterface private interface MetadataWriter {
        void write(IndexGeneration generation, int bookId, BookMetadata metadata);
    }
    @FunctionalInterface private interface MetricsWriter {
        void write(IndexGeneration generation, int bookId, int count);
    }
}
