package com.thebiggestdata.ingestion.infrastructure.port;

import jakarta.jms.Message;

public interface IngestionControlConsumer {
    void start() throws Exception;
    void onMessage(Message message);
}
