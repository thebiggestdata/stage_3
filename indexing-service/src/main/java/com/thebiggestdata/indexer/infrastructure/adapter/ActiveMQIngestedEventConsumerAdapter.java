package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.model.IngestionEvent;
import com.thebiggestdata.indexer.domain.port.IngestedEventConsumerPort;
import jakarta.jms.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ActiveMQIngestedEventConsumerAdapter implements IngestedEventConsumerPort {
    private final MessageConsumer consumer;
    private final Session session;
    private final BlockingQueue<IngestionEvent> queue = new LinkedBlockingQueue<>();

    public ActiveMQIngestedEventConsumerAdapter(ConnectionFactory factory, String queueName) {
        try {
            Connection connection = factory.createConnection();
            connection.start();
            this.session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            this.consumer = session.createConsumer(destination);
            this.consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String text = textMessage.getText();
                        String[] parts = text.split("\\|");

                        int bookId = Integer.parseInt(parts[0].trim());
                        String filePath = (parts.length > 1) ? parts[1].trim() : "";

                        IngestionEvent event = new IngestionEvent() {
                            @Override
                            public int bookId() { return bookId; }

                            @Override
                            public String path() { return filePath; }

                            @Override
                            public void acknowledge() {
                                try { message.acknowledge(); }
                                catch (JMSException e) { throw new RuntimeException(e); }
                            }

                            @Override
                            public void reject() {
                                try { session.recover(); }
                                catch (JMSException e) { throw new RuntimeException(e); }
                            }
                        };
                        queue.put(event);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing JMS message: " + e.getMessage());
                }
            });
        } catch (JMSException e) {
            throw new RuntimeException("Failed to initialize ActiveMQ consumer", e);
        }
    }

    @Override
    public IngestionEvent consumeBookId() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for ingestion event", e);
        }
    }
}