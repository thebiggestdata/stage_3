package com.thebiggestdata.indexer.domain.port;

public interface DatalakePathResolverPort {
    String resolveBodyPath(int bookId);
}
