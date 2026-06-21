package com.thebiggestdata.indexing.infrastructure.ports.old;

import java.util.function.Consumer;

public interface MessageConsumer {
    void startConsuming(Consumer<String> messageHandler);
}
