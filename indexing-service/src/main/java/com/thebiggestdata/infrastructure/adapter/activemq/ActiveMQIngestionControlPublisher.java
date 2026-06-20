package com.thebiggestdata.infrastructure.adapter.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.domain.gateway.IngestionSignalEmitter;
import com.thebiggestdata.domain.entity.IngestionSignal;
import jakarta.jms.*;

public class ActiveMQIngestionControlPublisher implements IngestionSignalEmitter {

    private final ConnectionFactory factory;
    private final Gson gson = new Gson();
    private static final String TOPIC_NAME = "ingestion.control";

    public ActiveMQIngestionControlPublisher(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void publishPause() {
        publish(IngestionSignal.Type.INGESTION_PAUSE);
    }

    public void publishResume() {
        publish(IngestionSignal.Type.INGESTION_RESUME);
    }

    private void publish(IngestionSignal.Type type) {
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(TOPIC_NAME);

            MessageProducer producer = session.createProducer(topic);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            IngestionSignal event = new IngestionSignal(type);

            String json = gson.toJson(event);
            TextMessage message = session.createTextMessage(json);

            producer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
