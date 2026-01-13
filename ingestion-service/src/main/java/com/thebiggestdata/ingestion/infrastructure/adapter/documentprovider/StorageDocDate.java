package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.hazelcast.map.IMap;
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
    private static final int REPLICATION_FACTOR = Integer.parseInt(System.getenv().getOrDefault("REPLICATION_FACTOR", "1"));
    private final PathProvider pathProvider;
    private final ContentSeparatorProvider contentSeparatorProvider;
    private final HazelcastDuplicationProvider hazelcastDuplicationProvider;

    public StorageDocDate(PathProvider pathProvider,
                          ContentSeparatorProvider contentSeparatorProvider,
                          HazelcastDuplicationProvider hazelcastDuplicationProvider) {
        this.pathProvider = pathProvider;
        this.contentSeparatorProvider = contentSeparatorProvider;
        this.hazelcastDuplicationProvider = hazelcastDuplicationProvider;
    }

    @Override
    public Path store(int bookId, String content) throws IOException {
        logger.debug("Starting storage process for book {}", bookId);
        List<String> contentSeparated = contentSeparatorProvider.provide(content);
        String header = contentSeparated.get(0);
        String body = contentSeparated.get(1);
        Path path = pathProvider.provide();
        Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
        Path bodyPath = path.resolve(String.format("%d_body.txt", bookId));
        Files.writeString(headerPath, header);
        Files.writeString(bodyPath, body);
        logger.info("Book {} saved to local partition: {}", bookId, path);
        IMap<Integer, Integer> replicaCount = hazelcastDuplicationProvider
                .getHazelcastInstance()
                .getMap("replication-count");
        replicaCount.put(bookId, 0);
        logger.debug("Initialized replica counter for book {}", bookId);
        int replicasConfirmed = hazelcastDuplicationProvider.duplicateAndWait(
                bookId, header, body, REPLICATION_FACTOR
        );
        if (replicasConfirmed < REPLICATION_FACTOR) {
            logger.error("Replication failed for book {}: only {} of {} replicas confirmed",
                    bookId, replicasConfirmed, REPLICATION_FACTOR);
            throw new IOException("Insufficient replicas for book " + bookId);
        }
        logger.info("Book {} replicated successfully to {} nodes", bookId, replicasConfirmed);
        return path;
    }
}

