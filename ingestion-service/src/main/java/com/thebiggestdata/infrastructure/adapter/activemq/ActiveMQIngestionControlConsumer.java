package com.thebiggestdata.infrastructure.adapter.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.usecase.IngestionPauseController;
import com.thebiggestdata.domain.gateway.IngestionControlConsumer;
import com.thebiggestdata.domain.entity.IngestionControlEvent;
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
            IngestionControlEvent event = gson.fromJson(json, IngestionControlEvent.class);

switch (event.type()) {
                case INGESTION_PAUSE -> pauseController.pause();
                case INGESTION_RESUME -> pauseController.resume();
            }

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
