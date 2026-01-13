package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast.HazelcastDuplicationProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ContentSeparatorProvider;
import com.thebiggestdata.ingestion.infrastructure.port.PathProvider;
import com.thebiggestdata.ingestion.infrastructure.port.StorageDocProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class StorageDocDate implements StorageDocProvider {

    private static final Logger logger = LoggerFactory.getLogger(StorageDocDate.class);

    private final PathProvider pathProvider;
    private final ContentSeparatorProvider contentSeparatorProvider;
    private final HazelcastDuplicationProvider hazelcastDuplicationProvider;

    public StorageDocDate(
            PathProvider pathProvider,
            ContentSeparatorProvider contentSeparatorProvider,
            HazelcastDuplicationProvider hazelcastDuplicationProvider) {
        this.pathProvider = pathProvider;
        this.contentSeparatorProvider = contentSeparatorProvider;
        this.hazelcastDuplicationProvider = hazelcastDuplicationProvider;
    }

    @Override
    public Path store(int bookId, String content) throws IOException {
        logger.debug("Storing book {} to disk and cluster", bookId);

        // Separate header and body
        List<String> contentSeparated = contentSeparatorProvider.provide(content);
        String header = contentSeparated.get(0);
        String body = contentSeparated.get(1);

        // Get storage path (organized by date/time)
        Path path = pathProvider.provide();
        Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
        Path bodyPath = path.resolve(String.format("%d_body.txt", bookId));

        // Write to local disk
        Files.writeString(headerPath, header);
        Files.writeString(bodyPath, body);
        logger.info("Book {} saved to disk: {}", bookId, path);

        // Replicate to Hazelcast cluster
        try {
            hazelcastDuplicationProvider.duplicate(bookId, header, body);
            logger.info("Book {} replicated to Hazelcast cluster", bookId);
        } catch (Exception e) {
            logger.error("Failed to replicate book {} to cluster: {}", bookId, e.getMessage());
            // Don't fail the entire operation if replication fails
            // The data is still safe on disk
        }

        return path;
    }
}