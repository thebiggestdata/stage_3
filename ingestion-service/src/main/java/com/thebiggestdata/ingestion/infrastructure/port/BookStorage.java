package com.thebiggestdata.ingestion.infrastructure.port;

import java.io.IOException;
import java.nio.file.Path;

public interface BookStorage {
    Path saveBook(int book_id, String[] content) throws IOException;
}
