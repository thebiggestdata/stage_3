package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.thebiggestdata.indexing.infrastructure.ports.RebuildCommandPublisher;
import com.thebiggestdata.indexing.model.RebuildCommand;

public final class ActiveMQRebuildCommandPublisher implements RebuildCommandPublisher {

    public static final String TOPIC = "index.rebuild.command";

    private final JmsTopicPublisher publisher;
    private final RebuildCommandMessageMapper mapper;

    public ActiveMQRebuildCommandPublisher(JmsTopicPublisher publisher, RebuildCommandMessageMapper mapper) {
        this.publisher = publisher;
        this.mapper = mapper;
    }

    @Override
    public void publish(RebuildCommand command) {
        publisher.publish(TOPIC, mapper.toJson(command));
    }
}
