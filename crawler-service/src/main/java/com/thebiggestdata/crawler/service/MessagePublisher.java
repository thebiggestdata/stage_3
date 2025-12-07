package com.thebiggestdata.crawler.service;

import com.thebiggestdata.crawler.model.BookContent;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);
    private final ConnectionFactory connectionFactory;
    private final String queueName;
    private final AtomicInteger sentCount = new AtomicInteger(0);

    public MessagePublisher(String brokerUrl, String queueName) {
        this.connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        this.queueName = queueName;
        logger.info("MessagePublisher initialized with queue: {}", queueName);
    }

    public void sendToIngestion(BookContent book) {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            connection.start();
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // Send message in format: bookId|content
            String message = book.bookId() + "|" + book.content();
            TextMessage textMessage = session.createTextMessage(message);
            
            producer.send(textMessage);
            sentCount.incrementAndGet();
            
            logger.info("Sent book {} to ingestion queue", book.bookId());
        } catch (JMSException e) {
            logger.error("Error sending message to broker: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending message to broker", e);
        }
    }

    public int getSentCount() {
        return sentCount.get();
    }
}
