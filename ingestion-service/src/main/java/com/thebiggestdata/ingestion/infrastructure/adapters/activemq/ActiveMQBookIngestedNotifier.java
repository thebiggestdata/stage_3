package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestedNotifier;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQBookIngestedNotifier implements BookIngestedNotifier {

    private final ConnectionFactory factory;

    public ActiveMQBookIngestedNotifier(String brokerUrl) {
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
            BookIngestedEvent event = new BookIngestedEvent(bookId);
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
