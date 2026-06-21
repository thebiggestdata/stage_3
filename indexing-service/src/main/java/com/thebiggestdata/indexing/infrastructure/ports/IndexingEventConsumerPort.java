package com.thebiggestdata.indexing.infrastructure.ports;

import java.util.function.Consumer;

public interface IndexingEventConsumerPort {
    void startConsuming(Consumer<String> messageHandler);
}
