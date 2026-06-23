package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.RebuildCommand;

public interface RebuildCommandPublisher {

    void publish(RebuildCommand command);
}
