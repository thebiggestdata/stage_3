package com.thebiggestdata.indexing.model;

public record RebuildCommand(
        String rebuildId,
        IndexGeneration targetGeneration,
        long requestedAtEpochMillis
) {
}
