package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import java.util.Map;

public final class JmsMessageSender implements AutoCloseable {

    private final ConnectionFactory connectionFactory;

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public JmsMessageSender(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public synchronized void sendToQueue(
            String queueName,
            String body,
            Map<String, Object> properties
    ) {
        try {
            ensureConnected();
            Queue queue = session.createQueue(queueName);
            TextMessage message = session.createTextMessage(body);
            properties.forEach((key, value) -> setProperty(message, key, value));

            producer.send(queue, message);
            session.commit();
        } catch (JMSException | RuntimeException e) {
            rollbackQuietly();
            closeResources();
            throw new ActiveMQAdapterException(
                    "Could not publish persistent message to queue " + queueName,
                    e
            );
        }
    }

    private void ensureConnected() throws JMSException {
        if (connection != null) {
            return;
        }

        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        producer = session.createProducer(null);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    private void setProperty(Message message, String key, Object value) {
        try {
            if (value instanceof String text) {
                message.setStringProperty(key, text);
            } else if (value instanceof Integer number) {
                message.setIntProperty(key, number);
            } else if (value instanceof Long number) {
                message.setLongProperty(key, number);
            } else if (value instanceof Boolean bool) {
                message.setBooleanProperty(key, bool);
            } else {
                message.setObjectProperty(key, value);
            }
        } catch (JMSException e) {
            throw new ActiveMQAdapterException("Could not set JMS property " + key, e);
        }
    }

    private void rollbackQuietly() {
        if (session == null) {
            return;
        }
        try {
            session.rollback();
        } catch (JMSException ignored) {
        }
    }

    @Override
    public synchronized void close() {
        closeResources();
    }

    private void closeResources() {
        close(producer);
        close(session);
        close(connection);
        producer = null;
        session = null;
        connection = null;
    }

    private void close(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception ignored) {
        }
    }
}
