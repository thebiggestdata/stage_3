package com.searchengine.indexer.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.searchengine.indexer.model.IndexingMessage;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BrokerConsumer.class);
    private final ConnectionFactory connectionFactory;
    private final String queueName;

    public BrokerConsumer(String brokerUrl, String queueName) {
        this.connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        this.queueName = queueName;
        logger.info("BrokerConsumer initialized with queue: {}", queueName);
    }

    public IndexingMessage consumeMessage() {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            connection.start();
            Destination destination = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(destination);

            Message message = consumer.receive();

            if (message instanceof TextMessage textMessage) {
                String payload = textMessage.getText();
                logger.debug("Received message: {}", payload);
                return parseMessage(payload);
            } else {
                logger.warn("Received non-text message, ignoring");
                return null;
            }
        } catch (JMSException e) {
            logger.error("Error consuming message from broker: {}", e.getMessage(), e);
            throw new RuntimeException("Error consuming message from broker", e);
        }
    }

    private IndexingMessage parseMessage(String payload) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);
        int bookId = jsonObject.get("bookId").getAsInt();
        return new IndexingMessage(bookId, null);
    }
}
