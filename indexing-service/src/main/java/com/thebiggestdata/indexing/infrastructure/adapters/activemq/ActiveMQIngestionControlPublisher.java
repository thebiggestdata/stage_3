package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.infrastructure.ports.IngestionControlPublisher;
import com.thebiggestdata.indexing.model.IngestionControlEvent;

public final class ActiveMQIngestionControlPublisher implements IngestionControlPublisher {

    private static final String TOPIC = "ingestion.control";

    private final JmsTopicPublisher publisher;
    private final Gson gson;

    public ActiveMQIngestionControlPublisher(JmsTopicPublisher publisher, Gson gson) {
        this.publisher = publisher;
        this.gson = gson;
    }

    @Override
    public void pause() {
        publish(IngestionControlEvent.Type.PAUSED);
    }

    @Override
    public void resume() {
        publish(IngestionControlEvent.Type.RESUMED);
    }

    private void publish(IngestionControlEvent.Type action) {
        publisher.publish(TOPIC, gson.toJson(new IngestionControlEvent(action)));
    }
}
