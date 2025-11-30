package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.DatalakePathResolverPort;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class LocalDatalakePathResolver implements DatalakePathResolverPort {

    @Override
    public String resolveBodyPath(int bookId) {

        String pattern = bookId + "_body.txt";
        Path datalakeRoot = Path.of("datalake");

        try (Stream<Path> stream = Files.walk(datalakeRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(pattern))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Body not found for " + bookId))
                    .toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed searching datalake", e);
        }
    }
}
