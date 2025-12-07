package com.thebiggestdata.crawler.service;

import com.thebiggestdata.crawler.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler {

    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);
    private final AtomicInteger crawledCount = new AtomicInteger(0);

    public BookContent crawl(String urlString, int bookId) {
        logger.info("Crawling URL: {}", urlString);
        
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Crawler/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                crawledCount.incrementAndGet();
                String title = extractTitle(urlString);
                logger.info("Successfully crawled content from {}, length: {}", urlString, content.length());
                
                return new BookContent(bookId, title, content.toString());
            } else {
                logger.error("Failed to crawl URL {}, response code: {}", urlString, responseCode);
                throw new RuntimeException("HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            logger.error("Error crawling URL: {}", urlString, e);
            throw new RuntimeException("Failed to crawl URL: " + urlString, e);
        }
    }

    private String extractTitle(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.isEmpty() ? "Book" : lastPart.replace(".txt", "").replace("-", " ");
    }

    public int getCrawledCount() {
        return crawledCount.get();
    }
}
