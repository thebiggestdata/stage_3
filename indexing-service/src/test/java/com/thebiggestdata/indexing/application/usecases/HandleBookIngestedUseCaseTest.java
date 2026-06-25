package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.ports.IndexingTracker;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndex;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStore;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildState;
import com.thebiggestdata.indexing.infrastructure.ports.TokenMetrics;
import com.thebiggestdata.indexing.model.BookContent;
import com.thebiggestdata.indexing.model.BookIngestedEvent;
import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingClaim;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandleBookIngestedUseCaseTest {

    private static final IndexGeneration GENERATION = new IndexGeneration("test");

    @Test
    void waitsForInProgressBooksInsteadOfFailingImmediately() {
        List<String> operations = new ArrayList<>();
        IndexBookUseCase indexBook = indexBook(
                operations,
                new SequencedTracker(operations, IndexingClaim.IN_PROGRESS, IndexingClaim.ALREADY_INDEXED)
        );
        HandleBookIngestedUseCase useCase = new HandleBookIngestedUseCase(
                indexBook,
                () -> operations.add("rebuild-ready"),
                Duration.ofSeconds(1),
                Duration.ofMillis(1)
        );

        useCase.execute(event());

        assertEquals(List.of("rebuild-ready", "claim", "claim", "remove"), operations);
    }

    @Test
    void failsOnlyWhenTheBookNeverLeavesInProgressState() {
        List<String> operations = new ArrayList<>();
        IndexBookUseCase indexBook = indexBook(
                operations,
                new SequencedTracker(operations, IndexingClaim.IN_PROGRESS, IndexingClaim.IN_PROGRESS)
        );
        HandleBookIngestedUseCase useCase = new HandleBookIngestedUseCase(
                indexBook,
                () -> {},
                Duration.ofMillis(5),
                Duration.ofMillis(1)
        );

        assertThrows(
                HandleBookIngestedUseCase.IndexingNotCompletedException.class,
                () -> useCase.execute(event())
        );
    }

    private BookIngestedEvent event() {
        return new BookIngestedEvent(42, "document.ingested", "2026-06-23T17:00:00Z", "source-node");
    }

    private IndexBookUseCase indexBook(List<String> operations, IndexingTracker tracker) {
        BookContentStore books = new BookContentStore() {
            @Override public BookContent get(int bookId) {
                operations.add("read");
                return new BookContent("header", "body");
            }
            @Override public void save(int bookId, BookContent content) {}
            @Override public void remove(int bookId) {
                operations.add("remove");
            }
        };
        IndexGenerationStore generations = new IndexGenerationStore() {
            @Override public IndexGeneration active() { return GENERATION; }
            @Override public void prepare(IndexGeneration generation) {}
            @Override public void activate(IndexGeneration generation) {}
        };
        InvertedIndex index = new InvertedIndex() {
            @Override public void addAll(IndexGeneration generation, List<IndexedTerm> terms) {
                operations.add("index");
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        MetadataStore metadata = new MetadataStore() {
            @Override public void save(IndexGeneration generation, int bookId, BookMetadata metadata) {
                operations.add("metadata");
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        TokenMetrics metrics = new TokenMetrics() {
            @Override public void record(IndexGeneration generation, int bookId, int tokenCount) {
                operations.add("metrics");
            }
            @Override public void clear(IndexGeneration generation) {}
        };
        return new IndexBookUseCase(
                books,
                index,
                tracker,
                generations,
                metadata,
                metrics,
                new TermFrequencyAnalyzer(text -> List.of("clean")),
                header -> new BookMetadata("Title", "Author", "English", 2026),
                "index-node"
        );
    }

    private static final class SequencedTracker implements IndexingTracker {
        private final List<String> operations;
        private final Queue<IndexingClaim> claims = new ArrayDeque<>();

        private SequencedTracker(List<String> operations, IndexingClaim... claims) {
            this.operations = operations;
            this.claims.addAll(List.of(claims));
        }

        @Override
        public IndexingClaim claim(IndexGeneration generation, int bookId) {
            operations.add("claim");
            return claims.isEmpty() ? IndexingClaim.IN_PROGRESS : claims.poll();
        }

        @Override public void complete(IndexGeneration generation, int bookId) {
            operations.add("complete");
        }

        @Override public void release(IndexGeneration generation, int bookId) {
            operations.add("release");
        }

        @Override public void clear(IndexGeneration generation) {}
    }
}
