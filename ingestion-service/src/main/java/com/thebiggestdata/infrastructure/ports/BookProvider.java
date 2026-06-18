package com.thebiggestdata.infrastructure.ports;

public interface BookProvider {
    String[] getBookContent(int bookId);
}