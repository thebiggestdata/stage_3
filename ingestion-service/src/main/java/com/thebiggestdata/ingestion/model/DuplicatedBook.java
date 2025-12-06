package com.thebiggestdata.ingestion.model;

public record DuplicatedBook (
        String header,
        String body,
        String srcNode
) {
}
