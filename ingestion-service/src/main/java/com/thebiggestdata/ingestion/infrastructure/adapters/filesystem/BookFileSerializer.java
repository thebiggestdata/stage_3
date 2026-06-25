package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

import com.thebiggestdata.ingestion.model.Book;

import java.nio.charset.StandardCharsets;

public final class BookFileSerializer {

    public BookFiles serialize(Book book) {
        return new BookFiles(
                book.content().header().getBytes(StandardCharsets.UTF_8),
                book.content().body().getBytes(StandardCharsets.UTF_8)
        );
    }

    public record BookFiles(byte[] header, byte[] body) {}
}