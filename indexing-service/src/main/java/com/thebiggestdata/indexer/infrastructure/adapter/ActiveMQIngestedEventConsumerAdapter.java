package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.IngestedEventConsumerPort;

import jakarta.jms.*;

public class ActiveMQIngestedEventConsumerAdapter implements IngestedEventConsumerPort {

    private final ConnectionFactory factory;
    private final String queueName;

    public ActiveMQIngestedEventConsumerAdapter(ConnectionFactory factory, String queueName) {
        this.factory = factory;
        this.queueName = queueName;
    }

    @Override
    public int consumeBookId() {
        try (
                Connection connection = factory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            connection.start();

            Destination destination = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(destination);

            Message msg = consumer.receive();

            if (msg instanceof TextMessage textMessage) {
                String payload = textMessage.getText();
                return Integer.parseInt(payload.split("\\|")[0]);
            }

            throw new RuntimeException("Invalid message type in MQ");

        } catch (Exception e) {
            throw new RuntimeException("Error consuming ingestion event", e);
        }
    }
}
