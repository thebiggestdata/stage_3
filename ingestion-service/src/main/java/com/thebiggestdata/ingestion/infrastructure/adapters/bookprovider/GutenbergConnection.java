package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class GutenbergConnection {
    private final String endPoint = "https://www.gutenberg.org/cache/epub/%d/pg%d.txt";

    public Connection createConnection(int bookId){
        return Jsoup.connect(String.format(endPoint, bookId, bookId))
                .ignoreContentType(true)
                .maxBodySize(0)
                .method(Connection.Method.GET);
    }
}
