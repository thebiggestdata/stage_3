package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.Book;

import java.util.stream.Stream;

public interface BookArchive {

    Stream<Book> books();
}
