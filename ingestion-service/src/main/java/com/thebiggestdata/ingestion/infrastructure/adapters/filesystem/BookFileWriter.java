package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BookFileWriter {

    public void write(Path path, byte[] content) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new FilesystemAdapterException("Could not write book file: " + path, e);
        }
    }
}