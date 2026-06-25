package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.BookArchive;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.Book;
import com.thebiggestdata.indexing.model.IndexingResult;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.RecoveryResult;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class RecoverIndexUseCase {

    private final BookArchive archive;
    private final BookContentStore liveBooks;
    private final IndexBookUseCase indexBook;

    public RecoverIndexUseCase(BookArchive archive, BookContentStore liveBooks, IndexBookUseCase indexBook) {
        this.archive = archive;
        this.liveBooks = liveBooks;
        this.indexBook = indexBook;
    }

    public RecoveryResult execute(IndexGeneration generation) {
        AtomicInteger recovered = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger maxBookId = new AtomicInteger();

        try (Stream<Book> books = archive.books()) {
            books.forEach(book -> {
                maxBookId.accumulateAndGet(book.bookId(), Math::max);
                liveBooks.save(book.bookId(), book.content());
                IndexingResult result = indexBook.execute(book.bookId(), generation, true);
                if (result.status() == IndexingResult.Status.FAILED) {
                    failed.incrementAndGet();
                } else {
                    recovered.incrementAndGet();
                }
            });
        }

        return new RecoveryResult(recovered.get(), maxBookId.get(), failed.get());
    }
}
