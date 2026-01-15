package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.port.DatalakeReadPort;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class FileSystemDatalakeReaderAdapter implements DatalakeReadPort {

    @Override
    public RawDocument read(int bookId, String bodyPath) {
        try {
            String body = Files.readString(Path.of(bodyPath));
            return new RawDocument(bookId, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + bodyPath, e);
        }
    }

}
