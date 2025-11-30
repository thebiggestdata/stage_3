package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.model.BookParts;
import com.thebiggestdata.ingestion.domain.port.EventPublisherPort;

import jakarta.jms.*;

public class ActiveMQPublisherAdapter implements EventPublisherPort {

    private final ConnectionFactory factory;
    private final String queueName;

    public ActiveMQPublisherAdapter(ConnectionFactory factory, String queueName) {
        this.factory = factory;
        this.queueName = queueName;
    }

    @Override
    public void publishIngestedEvent(BookParts parts) {
        try (
                Connection connection = factory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);

            TextMessage message = session.createTextMessage(
                    parts.bookId() + "|" + "INGESTED"
            );

            producer.send(message);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to publish event for book " + parts.bookId(),
                    e
            );
        }
    }
}
