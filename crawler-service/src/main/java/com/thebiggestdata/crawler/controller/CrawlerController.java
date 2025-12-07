package com.thebiggestdata.crawler.controller;

import com.google.gson.Gson;
import com.thebiggestdata.crawler.model.BookContent;
import com.thebiggestdata.crawler.model.CrawlRequest;
import com.thebiggestdata.crawler.service.MessagePublisher;
import com.thebiggestdata.crawler.service.WebCrawler;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerController.class);
    private final WebCrawler webCrawler;
    private final MessagePublisher messagePublisher;
    private final Gson gson;
    private String currentStatus = "idle";

    public CrawlerController(WebCrawler webCrawler, MessagePublisher messagePublisher) {
        this.webCrawler = webCrawler;
        this.messagePublisher = messagePublisher;
        this.gson = new Gson();
    }

    public void startCrawl(Context ctx) {
        try {
            String url = ctx.queryParam("url");
            if (url == null || url.isBlank()) {
                ctx.status(400).json(Map.of("status", "error", "message", "Missing 'url' parameter"));
                return;
            }

            String bookIdParam = ctx.queryParam("bookId");
            int bookId = bookIdParam != null ? Integer.parseInt(bookIdParam) : generateBookId(url);

            logger.info("Starting crawl for URL: {}, bookId: {}", url, bookId);
            currentStatus = "crawling";

            // Crawl in a separate thread to not block the response
            final int finalBookId = bookId;
            new Thread(() -> {
                try {
                    BookContent content = webCrawler.crawl(url, finalBookId);
                    messagePublisher.sendToIngestion(content);
                    currentStatus = "idle";
                    logger.info("Successfully crawled and sent book {} to ingestion", finalBookId);
                } catch (Exception e) {
                    logger.error("Error during crawl: {}", e.getMessage(), e);
                    currentStatus = "error";
                }
            }).start();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "started");
            response.put("url", url);
            response.put("bookId", bookId);

            ctx.status(202).json(response);
        } catch (Exception e) {
            logger.error("Error starting crawl", e);
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
            currentStatus = "error";
        }
    }

    public void getStatus(Context ctx) {
        Map<String, Object> status = new HashMap<>();
        status.put("status", currentStatus);
        status.put("crawled_count", webCrawler.getCrawledCount());
        status.put("sent_count", messagePublisher.getSentCount());

        ctx.status(200).json(status);
    }

    public void getStats(Context ctx) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("service", "crawler-service");
        stats.put("status", currentStatus);
        stats.put("total_crawled", webCrawler.getCrawledCount());
        stats.put("total_sent", messagePublisher.getSentCount());

        ctx.status(200).json(stats);
    }

    private int generateBookId(String url) {
        // Use unsigned right shift to ensure positive value
        return (url.hashCode() & 0x7FFFFFFF) % 100000;
    }
}
