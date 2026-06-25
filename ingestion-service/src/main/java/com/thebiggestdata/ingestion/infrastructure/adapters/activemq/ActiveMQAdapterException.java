package com.thebiggestdata.ingestion.infrastructure.adapters.activemq;

public final class ActiveMQAdapterException extends RuntimeException {

    public ActiveMQAdapterException(String message) {
        super(message);
    }

    public ActiveMQAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
