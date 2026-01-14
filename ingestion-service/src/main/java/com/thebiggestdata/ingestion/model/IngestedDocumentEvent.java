package com.thebiggestdata.ingestion.model;

import java.time.Instant;

public class IngestedDocumentEvent {
    private final int bookId;
    private final String ts = Instant.now().toString();
    private final String event = "ingested.document";

    public IngestedDocumentEvent(int bookId) {
        this.bookId = bookId;
    }

    public int getBookId() {return bookId;}
    public String getTs() {return ts;}
    public String getEvent() {return event;}
}

