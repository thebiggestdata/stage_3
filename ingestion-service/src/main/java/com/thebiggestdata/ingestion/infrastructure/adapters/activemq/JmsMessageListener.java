package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class JmsMessageListener implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JmsMessageListener.class);

    private final ConnectionFactory connectionFactory;
    private final ExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean();

    public JmsMessageListener(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ingestion-control-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void listenToDurableTopic(String topicName, String consumerId, Consumer<String> handler) {
        if (started.compareAndSet(false, true)) {
            executor.submit(() -> listen(topicName, consumerId, handler));
        }
    }

    private void listen(String topicName, String consumerId, Consumer<String> handler) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                consume(topicName, consumerId, handler);
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    log.warn("Ingestion control consumer disconnected; retrying", e);
                    sleepBeforeReconnect();
                }
            }
        }
    }

    private void consume(String topicName, String consumerId, Consumer<String> handler) throws JMSException {
        try (Connection connection = connectionFactory.createConnection()) {
            connection.setClientID(consumerId);
            connection.start();

            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Topic topic = session.createTopic(topicName);
                try (MessageConsumer consumer = session.createDurableConsumer(topic, consumerId)) {
                    receiveMessages(consumer, session, handler);
                }
            }
        }
    }

    private void receiveMessages(
            MessageConsumer consumer,
            Session session,
            Consumer<String> handler
    ) throws JMSException {
        while (!Thread.currentThread().isInterrupted()) {
            Message message = consumer.receive(1_000);
            if (message == null) {
                continue;
            }

            try {
                handle(message, handler);
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    private void handle(Message message, Consumer<String> handler) throws JMSException {
        if (!(message instanceof TextMessage textMessage)) {
            throw new ActiveMQAdapterException(
                    "Unsupported ingestion control message: " + message.getClass().getName()
            );
        }
        handler.accept(textMessage.getText());
    }

    private void sleepBeforeReconnect() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
