package com.thebiggestdata.ingestion.infrastructure.adapter.activemq;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;


public class ActiveMQBookConsumer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQBookConsumer.class);
    private static final String QUEUE_NAME = "books.to.ingest";
    private final String brokerUrl;
    private final DownloadDocumentProvider ingestService;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Gson gson = new Gson();

    public ActiveMQBookConsumer(String brokerUrl, DownloadDocumentProvider ingestService) {
        this.brokerUrl = brokerUrl;
        this.ingestService = ingestService;
    }

    @Override
    public void run() {
        logger.info("Starting ActiveMQ consumer for queue: {}", QUEUE_NAME);
        logger.info("Broker URL: {}", brokerUrl);
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            connectionFactory.setTrustedPackages(java.util.List.of("com.thebiggestdata"));
            connection = connectionFactory.createConnection();
            connection.start();
            logger.info("Connected to ActiveMQ broker");
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(QUEUE_NAME);
            consumer = session.createConsumer(destination);
            logger.info("Listening for messages on queue: {}", QUEUE_NAME);
            while (running.get()) {
                try {
                    Message message = consumer.receive(1000);
                    if (message instanceof TextMessage textMessage) {
                        String payload = textMessage.getText();
                        logger.debug("Received message: {}", payload);
                        int bookId = parseBookId(payload);
                        if (bookId > 0) {
                            logger.info("Processing book {} from queue", bookId);
                            var result = ingestService.ingest(bookId);
                            String status = (String) result.get("status");
                            if ("downloaded".equals(status) || "already_downloaded".equals(status)) {
                                logger.info("✓ Book {} processed successfully: {}", bookId, status);
                            } else {
                                logger.warn("✗ Book {} processing failed: {}", bookId, result);
                            }
                        }
                    }
                } catch (JMSException e) {
                    logger.error("Error receiving message: {}", e.getMessage());
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error in ActiveMQ consumer: {}", e.getMessage(), e);
        } finally {
            closeQuietly(consumer);
            closeQuietly(session);
            closeQuietly(connection);
            logger.info("ActiveMQ consumer stopped");
        }
    }

    private int parseBookId(String payload) {
        try {
            if (payload.contains("|")) {
                String[] parts = payload.split("\\|", 2);
                return Integer.parseInt(parts[0].trim());
            }
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            if (json != null && json.has("bookId")) {
                return json.get("bookId").getAsInt();
            }
            return Integer.parseInt(payload.trim());
        } catch (Exception e) {
            logger.error("Failed to parse bookId from message: {}", payload, e);
            return -1;
        }
    }

    public void stop() {
        logger.info("Stopping ActiveMQ consumer...");
        running.set(false);
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {closeable.close();}
            catch (Exception e) {
                logger.warn("Error closing resource: {}", e.getMessage());
            }
        }
    }
}