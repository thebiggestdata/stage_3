package com.thebiggestdata.indexing.infrastructure.adapters.filesystem;

import com.thebiggestdata.indexing.infrastructure.ports.BookArchive;
import com.thebiggestdata.indexing.model.Book;
import com.thebiggestdata.indexing.model.BookContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class FilesystemBookArchive implements BookArchive {

    private static final String BODY_SUFFIX = "_body.txt";
    private static final String HEADER_SUFFIX = "_header.txt";

    private final Path root;

    public FilesystemBookArchive(Path root) {
        this.root = root;
    }

    @Override
    public Stream<Book> books() {
        if (Files.notExists(root)) {
            return Stream.empty();
        }
        try {
            return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(BODY_SUFFIX))
                    .map(this::readBook);
        } catch (IOException e) {
            throw new FilesystemAdapterException("Could not scan datalake archive: " + root, e);
        }
    }

    private Book readBook(Path bodyPath) {
        int bookId = bookIdFrom(bodyPath);
        Path headerPath = bodyPath.resolveSibling(bookId + HEADER_SUFFIX);
        try {
            return new Book(
                    bookId,
                    new BookContent(Files.readString(headerPath), Files.readString(bodyPath))
            );
        } catch (IOException e) {
            throw new FilesystemAdapterException("Could not read archived book " + bookId, e);
        }
    }

    private int bookIdFrom(Path bodyPath) {
        String filename = bodyPath.getFileName().toString();
        return Integer.parseInt(filename.substring(0, filename.length() - BODY_SUFFIX.length()));
    }
}
