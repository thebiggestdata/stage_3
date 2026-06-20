package com.thebiggestdata.ingestion.infrastructure.ports.old;

import jakarta.jms.Message;

public interface IngestionControlConsumer {
    void start() throws Exception;
    void onMessage(Message message);
}
