package com.thebiggestdata.ingestion.infrastructure.port;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageDocProvider {
    Path store(int bookId, String content) throws IOException;
}
