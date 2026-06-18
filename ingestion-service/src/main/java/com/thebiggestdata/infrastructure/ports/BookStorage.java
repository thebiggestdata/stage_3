package com.thebiggestdata.infrastructure.ports;

import java.io.IOException;
import java.nio.file.Path;

public interface BookStorage {
    Path saveBook(int book_id, String[] content) throws IOException;
}
