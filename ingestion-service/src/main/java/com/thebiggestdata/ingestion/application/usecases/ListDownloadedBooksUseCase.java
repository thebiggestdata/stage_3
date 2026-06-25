package com.thebiggestdata.ingestion.application.usecases;

import com.thebiggestdata.ingestion.infrastructure.ports.DownloadedBooks;

import java.util.List;

public final class ListDownloadedBooksUseCase {

    private final DownloadedBooks downloadedBooks;

    public ListDownloadedBooksUseCase(DownloadedBooks downloadedBooks) {
        this.downloadedBooks = downloadedBooks;
    }

    public DownloadedBookList execute() {
        List<Integer> books = downloadedBooks.findAll();
        return new DownloadedBookList(books.size(), books);
    }

    public record DownloadedBookList(int count, List<Integer> books) {
        public DownloadedBookList {
            books = List.copyOf(books);
        }
    }
}
