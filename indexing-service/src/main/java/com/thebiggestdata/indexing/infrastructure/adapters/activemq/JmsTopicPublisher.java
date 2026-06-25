package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;

public final class JmsTopicPublisher implements AutoCloseable {

    private final ConnectionFactory connectionFactory;

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public JmsTopicPublisher(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public synchronized void publish(String topicName, String body) {
        try {
            ensureConnected();
            Topic topic = session.createTopic(topicName);
            TextMessage message = session.createTextMessage(body);
            producer.send(topic, message);
            session.commit();
        } catch (JMSException e) {
            rollbackQuietly();
            closeResources();
            throw new ActiveMQAdapterException("Could not publish to topic " + topicName, e);
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
