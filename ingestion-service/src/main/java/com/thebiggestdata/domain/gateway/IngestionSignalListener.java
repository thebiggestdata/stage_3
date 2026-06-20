package com.thebiggestdata.domain.gateway;

import jakarta.jms.Message;

public interface IngestionSignalListener {
    void start() throws Exception;
    void onMessage(Message message);
}
