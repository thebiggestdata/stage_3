package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

import com.thebiggestdata.ingestion.infrastructure.ports.BookIngestedPublisher;
import com.thebiggestdata.ingestion.model.BookIngestedEvent;

public final class ActiveMQBookIngestedPublisher implements BookIngestedPublisher {

    private static final String QUEUE = "documents.ingested";

    private final JmsMessageSender sender;
    private final BookIngestedMessageMapper mapper;

    public ActiveMQBookIngestedPublisher(JmsMessageSender sender, BookIngestedMessageMapper mapper) {
        this.sender = sender;
        this.mapper = mapper;
    }

    @Override
    public void publish(BookIngestedEvent event) {
        sender.sendToQueue(
                QUEUE,
                mapper.toJson(event),
                mapper.toProperties(event)
        );
    }

}
