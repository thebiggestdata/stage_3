package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

public final class HazelcastNames {

    public static final String INVERTED_INDEX = "inverted-index";
    public static final String BOOK_METADATA = "bookMetadata";
    public static final String INDEXED_BOOKS = "indexingRegistry";
    public static final String INDEX_GENERATIONS = "index-generations";

    private HazelcastNames() {}

    static String generated(String baseName, String generation) {
        return baseName + ":" + generation;
    }
}
