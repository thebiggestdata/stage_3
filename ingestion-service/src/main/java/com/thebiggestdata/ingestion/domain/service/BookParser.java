package com.thebiggestdata.ingestion.domain.service;

import com.thebiggestdata.ingestion.domain.exception.BookParsingException;
import com.thebiggestdata.ingestion.domain.model.BookContent;
import com.thebiggestdata.ingestion.domain.model.BookParts;

public class BookParser {

    private static final String START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK";
    private static final String END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK";

    public BookParts parse(BookContent content) {

        String raw = content.rawText();

        int start = raw.indexOf(START_MARKER);
        int end = raw.indexOf(END_MARKER);

        if (start == -1 || end == -1)
            throw new BookParsingException("Markers not found for book " + content.bookId());

        String header = raw.substring(0, start).trim();
        String body = raw.substring(start + START_MARKER.length(), end).trim();

        return new BookParts(content.bookId(), header, body);
    }
}
