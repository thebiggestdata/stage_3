package com.thebiggestdata.crawler;

import com.thebiggestdata.crawler.controller.CrawlerController;
import com.thebiggestdata.crawler.service.MessagePublisher;
import com.thebiggestdata.crawler.service.WebCrawler;
import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        System.out.println("Starting Crawler Service...");

        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String queueName = System.getenv().getOrDefault("INGESTION_QUEUE", "books.to.ingest");

        WebCrawler webCrawler = new WebCrawler();
        MessagePublisher messagePublisher = new MessagePublisher(brokerUrl, queueName);
        CrawlerController controller = new CrawlerController(webCrawler, messagePublisher);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7000);

        app.post("/crawl", controller::startCrawl);
        app.get("/crawl/status", controller::getStatus);
        app.get("/crawl/stats", controller::getStats);

        System.out.println("Crawler Service started on port 7000");
    }
}
