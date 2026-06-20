package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.application.usecases.ingestionservice.IngestionPauseController;
import com.thebiggestdata.ingestion.infrastructure.ports.IngestionControlConsumer;
import com.thebiggestdata.ingestion.model.IngestionStateEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQIngestionControlConsumer implements IngestionControlConsumer {

    private static final String TOPIC_NAME = "ingestion.control";

    private final String brokerUrl;
    private final Gson gson = new Gson();
    private final String consumerId;
    private final IngestionPauseController pauseController;

    public ActiveMQIngestionControlConsumer(String brokerUrl, String consumerId, IngestionPauseController pauseController) {
        this.brokerUrl = brokerUrl;
        this.consumerId = consumerId;
        this.pauseController = pauseController;
    }

    @Override
    public void start() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();
        connection.setClientID(consumerId);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(TOPIC_NAME);
        MessageConsumer consumer = session.createDurableConsumer(topic, consumerId);
        consumer.setMessageListener(this::onMessage);
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) return;

            String json = ((TextMessage) message).getText();
            IngestionStateEvent event = gson.fromJson(json, IngestionStateEvent.class);

switch (event.action()) {
                case PAUSED -> pauseController.pause();
                case RESUMED -> pauseController.resume();
            }

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
