package com.thebiggestdata.domain.gateway;

public interface BookSource {
    String[] getBookContent(int bookId);
}