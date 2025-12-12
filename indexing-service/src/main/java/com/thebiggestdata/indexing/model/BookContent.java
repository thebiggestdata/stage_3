package com.thebiggestdata.indexing.model;

public record BookContent(
        int bookId,
        String header,
        String body
) {
}