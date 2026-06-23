package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

public final class HazelcastNames {

    public static final String DATALAKE = "datalake";
    public static final String INVERTED_INDEX = "inverted-index";
    public static final String BOOK_METADATA = "bookMetadata";
    public static final String INDEXED_BOOKS = "indexingRegistry";
    public static final String INDEXING_IN_PROGRESS = "indexing-in-progress";
    public static final String TOKEN_COUNTS = "book-token-counts";
    public static final String INDEX_GENERATIONS = "index-generations";
    public static final String PENDING_BOOKS = "books";
    public static final String QUEUE_INITIALIZATION = "queue-initialization";
    public static final String REBUILD_STATE = "index-rebuild-state";
    public static final String REBUILD_EXPECTED = "index-rebuild-expected";
    public static final String REBUILD_COMPLETIONS = "index-rebuild-completions";
    public static final String REBUILD_MAX_BOOK_IDS = "index-rebuild-max-book-ids";
    public static final String REBUILD_FAILURES = "index-rebuild-failures";

    private HazelcastNames() {}

    static String generated(String baseName, String generation) {
        return baseName + ":" + generation;
    }
}
