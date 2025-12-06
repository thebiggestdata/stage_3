package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast.HazelcastDuplicationManager;
import com.thebiggestdata.ingestion.infrastructure.port.ContentSeparatorProvider;
import com.thebiggestdata.ingestion.infrastructure.port.PathProvider;
import com.thebiggestdata.ingestion.infrastructure.port.StorageDocProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StorageDocDate implements StorageDocProvider {
    private final PathProvider pathProvider;
    private final ContentSeparatorProvider contentSeparatorProvider;
    private final HazelcastDuplicationManager hazelcastDuplicationManager;

    public StorageDocDate(PathProvider pathProvider, ContentSeparatorProvider contentSeparatorProvider, HazelcastDuplicationManager hazelcastDuplicationManager) {
        this.pathProvider = pathProvider;
        this.contentSeparatorProvider = contentSeparatorProvider;
        this.hazelcastDuplicationManager = hazelcastDuplicationManager;
    }

    @Override
    public Path store(int bookId, String content) throws IOException {
        List<String> contentSeparated = contentSeparatorProvider.provide(content);
        String header = contentSeparated.get(0);
        String body = contentSeparated.get(1);
        Path path = pathProvider.provide();
        Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
        Path contentPath = path.resolve(String.format("%d_body.txt", bookId));
        Files.writeString(headerPath, header);
        Files.writeString(contentPath, body);
        this.hazelcastDuplicationManager.getHazelcastDuplicationProvider().duplicate(bookId,header,body);
        return path;
    }
}
