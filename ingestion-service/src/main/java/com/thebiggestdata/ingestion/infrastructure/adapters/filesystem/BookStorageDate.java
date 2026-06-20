package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.infrastructure.ports.PathGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BookStorageDate implements BookStorage {
    private final PathGenerator pathGenerator;

    public BookStorageDate(PathGenerator pathGenerator) {
        this.pathGenerator = pathGenerator;
    }

    @Override
    public Path saveBook(int bookId, String[] contentSeparated) throws IOException {
        String header = contentSeparated[0];
        String body = contentSeparated[1];

        Path path = pathGenerator.generatePath();

        Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
        Path contentPath = path.resolve(String.format("%d_body.txt", bookId));

        Files.writeString(headerPath, header);
        Files.writeString(contentPath, body);

        return path;
    }
}
