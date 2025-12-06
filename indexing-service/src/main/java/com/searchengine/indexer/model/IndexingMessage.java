package com.searchengine.indexer.model;

public record IndexingMessage(
        int bookId,
        String path
) {}

