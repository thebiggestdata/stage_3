package com.thebiggestdata.crawler.model;

public record CrawlStatus(
        String status,
        int crawledCount,
        int sentCount
) {}
