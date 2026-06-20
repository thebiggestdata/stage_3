package com.thebiggestdata.domain.gateway;

import java.io.IOException;
import java.nio.file.Path;

public interface BookArchive {
    Path saveBook(int book_id, String[] content) throws IOException;
}
