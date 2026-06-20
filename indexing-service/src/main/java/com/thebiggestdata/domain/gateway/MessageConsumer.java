package com.thebiggestdata.domain.gateway;

import java.util.function.Consumer;

public interface MessageConsumer {
    void startConsuming(Consumer<String> messageHandler);
}
