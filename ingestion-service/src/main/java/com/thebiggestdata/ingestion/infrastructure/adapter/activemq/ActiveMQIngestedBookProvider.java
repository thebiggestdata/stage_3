package com.thebiggestdata.ingestion.infrastructure.adapter.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentIngestedProvider;
import com.thebiggestdata.ingestion.model.IngestedDocumentEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQIngestedBookProvider implements DocumentIngestedProvider {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQIngestedBookProvider.class);
    private static final Gson gson = new Gson();
    private final String brokerUrl;

    public ActiveMQIngestedBookProvider(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    @Override
    public void provide(int bookId, String path) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("ingested.document");
            MessageProducer producer = session.createProducer(destination);

            IngestedDocumentEvent event = new IngestedDocumentEvent(bookId);
            String json = gson.toJson(event);
            TextMessage message = session.createTextMessage(json);
            producer.send(message);

            logger.info("Published ingestion event for book {}", bookId);

            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            logger.error("Failed to publish event for book {}", bookId, e);
        }
    }
}
