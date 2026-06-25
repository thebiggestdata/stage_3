package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.RebuildCommand;

import java.util.function.Consumer;

public interface RebuildCommandConsumer extends AutoCloseable {

    void start(Consumer<RebuildCommand> handler);

    @Override
    void close();
}
