package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookIngestedEvent;

import java.util.function.Consumer;

public interface IndexingEventConsumer extends AutoCloseable {

    void start(Consumer<BookIngestedEvent> handler);

    @Override
    void close();
}
