package com.thebiggestdata.indexer.domain.port;

public interface DatalakePathResolverPort {
    String resolve(int bookId);
}
