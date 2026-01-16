package com.thebiggestdata.ingestion.model;

import java.io.Serializable;

/**
 * Represents a book's content stored in the distributed datalake.
 * Shared model between services - MUST use same package name for Hazelcast serialization.
 */
public class BookContent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int bookId;
    private final String header;
    private final String body;
    private final long ingestedAt;
    private final String sourceNode;

    public BookContent(int bookId, String header, String body, String sourceNode) {
        this.bookId = bookId;
        this.header = header;
        this.body = body;
        this.ingestedAt = System.currentTimeMillis();
        this.sourceNode = sourceNode;
    }

    public int getBookId() {
        return bookId;
    }

    public String getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }

    public long getIngestedAt() {
        return ingestedAt;
    }

    public String getSourceNode() {
        return sourceNode;
    }

    @Override
    public String toString() {
        return "BookContent{bookId=" + bookId + ", sourceNode='" + sourceNode + "', bodyLength=" + (body != null ? body.length() : 0) + "}";
    }
}
