package com.thebiggestdata.ingestion.infrastructure.adapters.web;

import io.javalin.http.Context;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.application.usecases.GetBookStatusUseCase;
import com.thebiggestdata.ingestion.application.usecases.IngestBookUseCase;
import com.thebiggestdata.ingestion.application.usecases.ListDownloadedBooksUseCase;
import com.thebiggestdata.ingestion.model.IngestionResult;

public final class BookProviderController {

    private final IngestBookUseCase ingestBook;
    private final ListDownloadedBooksUseCase listDownloadedBooks;
    private final GetBookStatusUseCase getBookStatus;
    private final Gson gson;

    public BookProviderController(
            IngestBookUseCase ingestBook,
            ListDownloadedBooksUseCase listDownloadedBooks,
            GetBookStatusUseCase getBookStatus,
            Gson gson
    ) {
        this.ingestBook = ingestBook;
        this.listDownloadedBooks = listDownloadedBooks;
        this.getBookStatus = getBookStatus;
        this.gson = gson;
    }

    public void ingestBook(Context ctx) {
        int bookId = positiveBookId(ctx);
        IngestionResult result = ingestBook.execute(bookId);
        ctx.status(switch (result.status()) {
            case IN_PROGRESS -> 202;
            case PAUSED -> 503;
            default -> 200;
        });
        ctx.result(gson.toJson(result));
    }

    public void listAllBooks(Context ctx) {
        ctx.result(gson.toJson(listDownloadedBooks.execute()));
    }

    public void status(Context ctx) {
        ctx.result(gson.toJson(getBookStatus.execute(positiveBookId(ctx))));
    }

    private int positiveBookId(Context context) {
        int bookId = Integer.parseInt(context.pathParam("book_id"));
        if (bookId < 1) {
            throw new IllegalArgumentException("book_id must be positive");
        }
        return bookId;
    }
}
