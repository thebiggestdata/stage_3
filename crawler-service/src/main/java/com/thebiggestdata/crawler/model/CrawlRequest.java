package com.thebiggestdata.crawler.model;

public record CrawlRequest(
        String url,
        int bookId
) {}
