package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import com.thebiggestdata.ingestion.model.BookContent;

public class GutenbergBookParser {

    public BookContent parse(String rawBook) {
        int bodyStart = rawBook.indexOf("*** START");
        int bodyEnd = rawBook.indexOf("*** END");

        if (bodyStart < 0 || bodyEnd < 0 || bodyStart >= bodyEnd) {
            return new BookContent("", rawBook);
        }

        String header = rawBook.substring(0, bodyStart).trim();
        String body = rawBook.substring(bodyStart, bodyEnd).trim();

        return new BookContent(header, body);
    }

}
