package com.thebiggestdata.infrastructure.adapter.web;

import com.google.gson.Gson;
import com.thebiggestdata.usecase.IngestBook;
import com.thebiggestdata.domain.gateway.BookListProvider;
import com.thebiggestdata.domain.gateway.BookStatusProvider;
import io.javalin.http.Context;

import java.util.Map;

public class BookProviderController {
    private final IngestBook ingestBookUseCase;

    private final BookListProvider listBooksService;
    private final BookStatusProvider bookStatusService;
    private static final Gson gson = new Gson();


    public BookProviderController(IngestBook ingestBookUseCase, BookListProvider listBooksService, BookStatusProvider bookStatusService) {
        this.ingestBookUseCase = ingestBookUseCase;
        this.listBooksService = listBooksService;
        this.bookStatusService = bookStatusService;
    }

    public void ingestBook(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = ingestBookUseCase.execute(bookId);
        ctx.result(gson.toJson(result));
    }

    public void listAllBooks(Context ctx) {
        Map<String, Object> result = listBooksService.getBookList();
        ctx.result(gson.toJson(result));
    }

    public void status(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = bookStatusService.getBookStatus(bookId);
        ctx.result(gson.toJson(result));
    }
}
