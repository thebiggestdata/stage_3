package com.thebiggestdata.indexing.infrastructure.adapters.recovery;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentArchive;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentNotFoundException;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.BookContent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveringBookContentStoreTest {

    @Test
    void restoresMissingLiveBookFromArchiveBeforeReturningIt() {
        LiveStore liveStore = new LiveStore();
        BookContent archived = new BookContent("header", "body");
        BookContentArchive archive = bookId -> Optional.of(archived);
        RecoveringBookContentStore store = new RecoveringBookContentStore(liveStore, archive);

        BookContent content = store.get(42);

        assertEquals(archived, content);
        assertEquals(42, liveStore.savedBookId);
        assertEquals(archived, liveStore.savedContent);
    }

    @Test
    void marksTheMissAsUnrecoverableWhenArchiveDoesNotContainTheBook() {
        RecoveringBookContentStore store = new RecoveringBookContentStore(
                new LiveStore(),
                bookId -> Optional.empty()
        );

        BookContentNotFoundException exception = assertThrows(
                BookContentNotFoundException.class,
                () -> store.get(42)
        );

        assertEquals("Book not found in live datalake or archive: 42", exception.getMessage());
    }

    private static final class LiveStore implements BookContentStore {
        private int savedBookId;
        private BookContent savedContent;

        @Override
        public BookContent get(int bookId) {
            throw new BookContentNotFoundException("Book not found in live datalake: " + bookId);
        }

        @Override
        public void save(int bookId, BookContent content) {
            this.savedBookId = bookId;
            this.savedContent = content;
        }

        @Override
        public void remove(int bookId) {
        }
    }
}
