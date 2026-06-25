package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

public final class HazelcastNames {

    public static final String DATALAKE = "datalake";
    public static final String DOWNLOADED_BOOKS = "downloaded-books";
    public static final String INGESTIONS_IN_PROGRESS = "ingestions-in-progress";
    public static final String PENDING_BOOKS = "books";
    public static final String INGESTION_ATTEMPTS = "ingestion-attempts";
    public static final String FAILED_INGESTIONS = "failed-ingestions";
    public static final String INDEXED_BOOKS = "indexingRegistry";
    public static final String INDEX_GENERATIONS = "index-generations";
    public static final String INVERTED_INDEX = "inverted-index";
    public static final String BOOK_METADATA = "bookMetadata";
    public static final String INDEXING_IN_PROGRESS = "indexing-in-progress";
    public static final String TOKEN_COUNTS = "book-token-counts";
    public static final String REPLICATION_QUEUE = "booksToBeReplicated";
    public static final String REPLICATED_NODES = "replicatedNodesMap";

    private HazelcastNames() {}

    public static String replicationQueueFor(String nodeId) {
        return REPLICATION_QUEUE + ":" + nodeId;
    }
}
