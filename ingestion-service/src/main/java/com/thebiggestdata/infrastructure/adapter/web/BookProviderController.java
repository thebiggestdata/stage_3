package com.thebiggestdata.infrastructure.adapter.web;

import com.google.gson.Gson;
import com.thebiggestdata.usecase.IngestBookUseCase;
import com.thebiggestdata.domain.gateway.BookCatalogProvider;
import com.thebiggestdata.domain.gateway.BookStatusReader;
import io.javalin.http.Context;

import java.util.Map;

public class BookProviderController {
    private final IngestBookUseCase ingestBookUseCase;

    private final BookCatalogProvider listBooksService;
    private final BookStatusReader bookStatusService;
    private static final Gson gson = new Gson();


    public BookProviderController(IngestBookUseCase ingestBookUseCase, BookCatalogProvider listBooksService, BookStatusReader bookStatusService) {
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
