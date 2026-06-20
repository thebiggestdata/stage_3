package com.thebiggestdata.domain.gateway;

import java.util.function.Consumer;

public interface EventListener {
    void startConsuming(Consumer<String> messageHandler);
}
