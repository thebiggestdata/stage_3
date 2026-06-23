package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.model.BookIngestedEvent;
import com.thebiggestdata.indexing.model.IndexingResult;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildState;

public final class HandleBookIngestedUseCase {

    private final IndexBookUseCase indexBook;
    private final RebuildState rebuildState;

    public HandleBookIngestedUseCase(IndexBookUseCase indexBook, RebuildState rebuildState) {
        this.indexBook = indexBook;
        this.rebuildState = rebuildState;
    }

    public void execute(BookIngestedEvent event) {
        rebuildState.awaitCompletion();
        IndexingResult result = indexBook.execute(event.bookId(), event.sourceNodeId());
        if (result.status() == IndexingResult.Status.FAILED
                || result.status() == IndexingResult.Status.IN_PROGRESS) {
            throw new IndexingNotCompletedException(result);
        }
    }

    public static final class IndexingNotCompletedException extends RuntimeException {
        public IndexingNotCompletedException(IndexingResult result) {
            super("Indexing did not complete for book %d: %s"
                    .formatted(result.bookId(), result.status()));
        }
    }
}
