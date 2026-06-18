package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.DatalakePathResolverPort;

public class LocalDatalakePathResolver implements DatalakePathResolverPort {
    private final String basePath;

    public LocalDatalakePathResolver() {
        this.basePath = System.getenv().getOrDefault("DATALAKE_BASE_PATH", "/app/datalake");
    }

    @Override
    public String resolve(int bookId) {
        return basePath + "/" + bookId + ".txt";
    }
}