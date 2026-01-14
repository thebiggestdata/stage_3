package com.thebiggestdata.ingestion.infrastructure.adapter.activemq;
import com.google.gson.Gson;
import com.thebiggestdata.ingestion.model.IngestedDocumentEvent;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentIngestedProvider;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ActiveMQIngestedBookProvider implements DocumentIngestedProvider {
    private static final Logger log = LoggerFactory.getLogger(ActiveMQIngestedBookProvider.class);
    private final ConnectionFactory connectionFactory;
    private final boolean enabled;

    public ActiveMQIngestedBookProvider(String brokerUrl) {
        if (brokerUrl == null || brokerUrl.isEmpty() || brokerUrl.equalsIgnoreCase("disabled")) {
            this.connectionFactory = null;
            this.enabled = false;
            System.out.println("[ActiveMQ] Messaging disabled - no broker URL provided");
        } else {
            this.connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            this.enabled = true;
            System.out.println("[ActiveMQ] Configured with broker URL: " + brokerUrl);
        }
    }

    @Override
    public void provide(int bookId) {
        if (!enabled || connectionFactory == null) {
            System.out.println("[ActiveMQ] Skipping message for book " + bookId + " - messaging disabled");
            return;
        }
        try (Connection connection = connectionFactory.createConnection()) {
            Gson gson = new Gson();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue("ingested.documents");
            MessageProducer producer = session.createProducer(queue);
            IngestedDocumentEvent event = new IngestedDocumentEvent(bookId);
            String jsonMessage = gson.toJson(event);
            TextMessage message = session.createTextMessage(jsonMessage);
            producer.send(message);
            log.info("[ingested.documents] Message sent: {}", jsonMessage);
            session.close();
        } catch (JMSException e) {
            System.err.println("[ActiveMQ] Warning: Could not send message for book " + bookId + ": " + e.getMessage());
        }
    }
}
