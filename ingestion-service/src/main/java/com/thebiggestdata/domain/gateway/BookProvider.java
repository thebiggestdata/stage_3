package com.thebiggestdata.domain.gateway;

public interface BookProvider {
    String[] getBookContent(int bookId);
}