package com.thebiggestdata.ingestion.infrastructure.adapter.activemq;

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
        } else {
            this.connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            this.enabled = true;
        }
    }

    @Override
    public void provide(int bookId, String filePath) {
        if (!enabled || connectionFactory == null) return;

        try (Connection connection = connectionFactory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue("ingested.document");
            MessageProducer producer = session.createProducer(queue);

            String payload = bookId + "|" + filePath;

            TextMessage message = session.createTextMessage(payload);
            producer.send(message);

            log.info("Sent ActiveMQ message: {}", payload);
            session.close();
        } catch (JMSException e) {
            log.error("Failed to send message for book {}", bookId, e);
        }
    }
}