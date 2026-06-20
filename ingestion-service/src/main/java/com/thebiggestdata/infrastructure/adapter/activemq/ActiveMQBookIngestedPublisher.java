package com.thebiggestdata.infrastructure.adapter.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.domain.gateway.BookIngestedPublisher;
import com.thebiggestdata.domain.entity.DocumentReceivedEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQBookIngestedPublisher implements BookIngestedPublisher {

    private final ConnectionFactory factory;

    public ActiveMQBookIngestedPublisher(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    @Override
    public void notifyIngestedBook(int bookId) {
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue("documents.ingested");
            MessageProducer producer = session.createProducer(queue);

            Gson gson = new Gson();
            DocumentReceivedEvent event = new DocumentReceivedEvent(bookId);
            String json = gson.toJson(event);

            TextMessage message = session.createTextMessage(json);
            producer.send(message);

            System.out.println("[documents.ingested] Message sent: " + json);

            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
