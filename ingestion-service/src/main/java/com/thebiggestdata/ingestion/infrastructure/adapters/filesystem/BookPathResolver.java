package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

import java.nio.file.Path;

public final class BookPathResolver {

    private final Path root;

    public BookPathResolver(Path root) {
        this.root = root;
    }

    public BookPath resolve(int bookId) {
        String id = String.valueOf(bookId);
        Path shard = root.resolve(shard(id));

        return new BookPath(
                shard.resolve(id + "_header.txt"),
                shard.resolve(id + "_body.txt")
        );
    }

    private String shard(String id) {
        return id.length() == 1 ? "0" + id : id.substring(0, 2);
    }

    public record BookPath(Path headerPath, Path bodyPath) {}

}