package com.thebiggestdata.indexer.domain.service;

import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.exception.DocumentReadException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class DatalakeReader {

    public RawDocument read(int bookId, String bodyPath) {
        try {
            String body = Files.readString(Path.of(bodyPath));
            return new RawDocument(bookId, body);
        } catch (IOException e) {
            throw new DocumentReadException("Failed to read body for book " + bookId, e);
        }
    }
}
