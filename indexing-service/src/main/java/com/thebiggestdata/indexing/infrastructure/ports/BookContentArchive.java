package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookContent;

import java.util.Optional;

public interface BookContentArchive {

    Optional<BookContent> find(int bookId);
}
