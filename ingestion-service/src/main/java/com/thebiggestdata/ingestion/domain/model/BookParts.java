package com.thebiggestdata.ingestion.domain.model;

public record BookParts(
        int bookId,
        String header,
        String body
) {
}
