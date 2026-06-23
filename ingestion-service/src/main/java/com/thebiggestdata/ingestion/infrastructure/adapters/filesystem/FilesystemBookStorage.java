package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.BookFileSerializer.BookFiles;
import com.thebiggestdata.ingestion.infrastructure.adapters.filesystem.BookPathResolver.BookPath;
import com.thebiggestdata.ingestion.model.Book;

public class FilesystemBookStorage implements BookStorage {

    private final BookPathResolver pathResolver;
    private final BookFileSerializer serializer;
    private final BookFileWriter writer;

    public FilesystemBookStorage(BookPathResolver pathResolver, BookFileSerializer serializer, BookFileWriter writer) {
        this.pathResolver = pathResolver;
        this.serializer = serializer;
        this.writer = writer;
    }

    @Override
    public void save(Book book) {
        BookFiles files = serializer.serialize(book);
        BookPath path = pathResolver.resolve(book.bookId());

        writer.write(path.headerPath(), files.header());
        writer.write(path.bodyPath(), files.body());
    }
}
