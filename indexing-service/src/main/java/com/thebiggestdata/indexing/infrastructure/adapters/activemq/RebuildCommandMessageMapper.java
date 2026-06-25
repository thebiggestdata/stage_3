package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.thebiggestdata.indexing.model.RebuildCommand;

public final class RebuildCommandMessageMapper {

    private final Gson gson;

    public RebuildCommandMessageMapper(Gson gson) {
        this.gson = gson;
    }

    public String toJson(RebuildCommand command) {
        return gson.toJson(command);
    }

    public RebuildCommand fromJson(String json) {
        try {
            RebuildCommand command = gson.fromJson(json, RebuildCommand.class);
            if (command.rebuildId() == null || command.rebuildId().isBlank()) {
                throw new IllegalArgumentException("Missing rebuildId");
            }
            if (command.targetGeneration() == null) {
                throw new IllegalArgumentException("Missing targetGeneration");
            }
            return command;
        } catch (RuntimeException e) {
            throw new ActiveMQAdapterException("Invalid index rebuild command", e);
        }
    }
}
