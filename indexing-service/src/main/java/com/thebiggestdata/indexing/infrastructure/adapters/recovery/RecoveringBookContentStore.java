package com.thebiggestdata.indexing.infrastructure.adapters.recovery;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentArchive;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentNotFoundException;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecoveringBookContentStore implements BookContentStore {

    private static final Logger log = LoggerFactory.getLogger(RecoveringBookContentStore.class);

    private final BookContentStore liveStore;
    private final BookContentArchive archive;

    public RecoveringBookContentStore(BookContentStore liveStore, BookContentArchive archive) {
        this.liveStore = liveStore;
        this.archive = archive;
    }

    @Override
    public BookContent get(int bookId) {
        try {
            return liveStore.get(bookId);
        } catch (BookContentNotFoundException e) {
            return restoreFromArchive(bookId, e);
        }
    }

    @Override
    public void save(int bookId, BookContent content) {
        liveStore.save(bookId, content);
    }

    @Override
    public void remove(int bookId) {
        liveStore.remove(bookId);
    }

    private BookContent restoreFromArchive(int bookId, BookContentNotFoundException liveMiss) {
        return archive.find(bookId)
                .map(content -> {
                    liveStore.save(bookId, content);
                    log.info("INDEXING_DATALAKE_RESTORED bookId={} source=archive", bookId);
                    return content;
                })
                .orElseThrow(() -> {
                    log.warn("INDEXING_DATALAKE_ARCHIVE_MISS bookId={}", bookId);
                    return new BookContentNotFoundException(
                            "Book not found in live datalake or archive: " + bookId,
                            liveMiss
                    );
                });
    }
}
