package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookContent;

import java.io.IOException;
import java.nio.file.Path;

public interface BookStoragePort {
    Path save(int bookId, BookContent content) throws IOException;

}
