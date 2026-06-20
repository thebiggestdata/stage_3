package com.thebiggestdata.indexing.infrastructure.ports;

import java.util.function.Consumer;

public interface MessageConsumer {
    void startConsuming(Consumer<String> messageHandler);
}
