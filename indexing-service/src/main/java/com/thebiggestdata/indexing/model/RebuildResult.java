package com.thebiggestdata.indexing.model;

public record RebuildResult(boolean success, String rebuildId, String message) {
}
