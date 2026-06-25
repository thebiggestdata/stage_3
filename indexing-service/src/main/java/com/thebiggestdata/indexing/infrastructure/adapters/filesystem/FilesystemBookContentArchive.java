package com.thebiggestdata.indexing.infrastructure.adapters.filesystem;

import com.thebiggestdata.indexing.infrastructure.ports.BookContentArchive;
import com.thebiggestdata.indexing.model.BookContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FilesystemBookContentArchive implements BookContentArchive {

    private static final String BODY_SUFFIX = "_body.txt";
    private static final String HEADER_SUFFIX = "_header.txt";

    private final Path root;

    public FilesystemBookContentArchive(Path root) {
        this.root = root;
    }

    @Override
    public Optional<BookContent> find(int bookId) {
        Path headerPath = path(bookId, HEADER_SUFFIX);
        Path bodyPath = path(bookId, BODY_SUFFIX);
        if (Files.notExists(headerPath) || Files.notExists(bodyPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BookContent(Files.readString(headerPath), Files.readString(bodyPath)));
        } catch (IOException e) {
            throw new FilesystemAdapterException("Could not read archived book " + bookId, e);
        }
    }

    private Path path(int bookId, String suffix) {
        String id = String.valueOf(bookId);
        return root.resolve(shard(id)).resolve(id + suffix);
    }

    private String shard(String id) {
        return id.length() == 1 ? "0" + id : id.substring(0, 2);
    }
}
