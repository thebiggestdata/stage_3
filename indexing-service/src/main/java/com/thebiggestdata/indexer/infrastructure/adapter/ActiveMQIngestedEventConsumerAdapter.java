package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.model.IngestionEvent;
import com.thebiggestdata.indexer.domain.port.IngestedEventConsumerPort;
import jakarta.jms.*;

public class ActiveMQIngestedEventConsumerAdapter implements IngestedEventConsumerPort {
    private final MessageConsumer consumer;
    private final Session session;

    public ActiveMQIngestedEventConsumerAdapter(ConnectionFactory factory, String queueName) {
        try {
            Connection connection = factory.createConnection();
            connection.start();
            this.session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);            Destination destination = session.createQueue(queueName);
            this.consumer = session.createConsumer(destination);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to initialize ActiveMQ consumer", e);
        }
    }

    @Override
    public IngestionEvent consumeBookId() {
        try {
            Message message = consumer.receive();
            if (message instanceof TextMessage textMessage) {
                String text = textMessage.getText();
                int bookId = Integer.parseInt(text.split("\\|")[0]);
                return new IngestionEvent() {
                    @Override
                    public int bookId() {
                        return bookId;
                    }
                    @Override
                    public void acknowledge() {
                        try {message.acknowledge();}
                        catch (JMSException e) {throw new RuntimeException(e);}
                    }

                    @Override
                    public void reject() {
                        try {session.recover();}
                        catch (JMSException e) {throw new RuntimeException(e);}
                    }
                };
            } throw new RuntimeException("Unknown message type");
        } catch (JMSException e) {
            throw new RuntimeException("Error receiving message",e);
        }
    }
}
