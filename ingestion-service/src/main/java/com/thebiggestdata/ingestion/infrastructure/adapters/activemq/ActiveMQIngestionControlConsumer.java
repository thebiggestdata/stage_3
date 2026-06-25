package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.thebiggestdata.ingestion.infrastructure.ports.IngestionState;

public final class ActiveMQIngestionControlConsumer {

    private static final String TOPIC = "ingestion.control";

    private final JmsMessageListener listener;
    private final IngestionControlMessageMapper mapper;
    private final IngestionState ingestionState;

    public ActiveMQIngestionControlConsumer(JmsMessageListener listener, IngestionControlMessageMapper mapper, IngestionState ingestionState) {
        this.listener = listener;
        this.mapper = mapper;
        this.ingestionState = ingestionState;
    }

    public void start(String consumerId) {
        listener.listenToDurableTopic(TOPIC, consumerId, message -> {
            switch (mapper.toEvent(message).action()) {
                case PAUSED -> ingestionState.pause();
                case RESUMED -> ingestionState.resume();
            }
        });
    }
}
