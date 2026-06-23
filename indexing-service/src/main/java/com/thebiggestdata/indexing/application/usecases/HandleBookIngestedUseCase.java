package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.model.BookIngestedEvent;
import com.thebiggestdata.indexing.model.IndexingResult;
import com.thebiggestdata.indexing.infrastructure.ports.RebuildState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public final class HandleBookIngestedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleBookIngestedUseCase.class);

    private final IndexBookUseCase indexBook;
    private final RebuildState rebuildState;
    private final Duration inProgressTimeout;
    private final Duration inProgressRetryDelay;

    public HandleBookIngestedUseCase(
            IndexBookUseCase indexBook,
            RebuildState rebuildState,
            Duration inProgressTimeout,
            Duration inProgressRetryDelay
    ) {
        this.indexBook = indexBook;
        this.rebuildState = rebuildState;
        this.inProgressTimeout = inProgressTimeout;
        this.inProgressRetryDelay = inProgressRetryDelay;
    }

    public void execute(BookIngestedEvent event) {
        rebuildState.awaitCompletion();
        IndexingResult result = indexUntilCompleted(event);
        if (result.status() == IndexingResult.Status.FAILED) {
            throw new IndexingNotCompletedException(result);
        }
    }

    private IndexingResult indexUntilCompleted(BookIngestedEvent event) {
        Instant deadline = Instant.now().plus(inProgressTimeout);
        IndexingResult result;
        boolean waitingWasLogged = false;

        do {
            result = indexBook.execute(event.bookId(), event.sourceNodeId());
            if (result.status() != IndexingResult.Status.IN_PROGRESS) {
                return result;
            }
            if (!waitingWasLogged) {
                log.info("INDEXING_WAITING bookId={} reason=in-progress", event.bookId());
                waitingWasLogged = true;
            }
            waitBeforeRetrying();
        } while (Instant.now().isBefore(deadline));

        throw new IndexingNotCompletedException(result);
    }

    private void waitBeforeRetrying() {
        try {
            Thread.sleep(inProgressRetryDelay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for in-progress indexing", e);
        }
    }

    public static final class IndexingNotCompletedException extends RuntimeException {
        public IndexingNotCompletedException(IndexingResult result) {
            super("Indexing did not complete for book %d: %s"
                    .formatted(result.bookId(), result.status()));
        }
    }
}
