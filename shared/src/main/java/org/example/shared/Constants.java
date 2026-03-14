package org.example.shared;

public final class Constants {

    private Constants() {}

    // Hazelcast distributed data structure names
    public static final String INVERTED_INDEX_MAP  = "inverted-index";
    public static final String BOOK_METADATA_MAP   = "book-metadata";
    public static final String BOOK_DOWNLOADED_TOPIC = "book-downloaded";

    // Hazelcast cluster group
    public static final String CLUSTER_NAME = "distributed-search-engine";

    // Gutenberg base URL
    public static final String GUTENBERG_BASE_URL =
            "https://www.gutenberg.org/cache/epub/%s/pg%s.txt";

    // Datalake root folder (relative to working directory)
    public static final String DATALAKE_ROOT = "datalake";

    // Gutenberg text markers
    public static final String BODY_START_MARKER = "*** START OF";
    public static final String BODY_END_MARKER   = "*** END OF";

    // Replication factor
    public static final int REPLICATION_FACTOR = 2;
}
