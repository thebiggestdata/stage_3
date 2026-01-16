package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.ingestion.infrastructure.adapter.api.InitialBookLoader;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DateTimePathProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public class HazelcastDatalakeListener extends HzlcstEntryListener<Integer, DuplicatedBook> {
    private static final Logger logger = LoggerFactory.getLogger(InitialBookLoader.class);
    private final NodeIdProvider nodeIdProvider;
    private final int replicationFactor;
    private final HazelcastInstance hazelcast;
    private final String datalakeBasePath = System.getenv().getOrDefault("DATALAKE_PATH", "/app/datalake");

    public HazelcastDatalakeListener(HazelcastInstance hazelcast, NodeIdProvider nodeIdProvider, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeIdProvider = nodeIdProvider;
        this.replicationFactor = replicationFactor;
    }

    public void registerListener() {
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");
        datalake.addEntryListener(this, true);
    }

    @Override
    public void entryAdded(EntryEvent<Integer, DuplicatedBook> event) {
        DuplicatedBook replicated = event.getValue();
        int bookId = event.getKey();
        if (replicated == null || replicated.srcNode() == null) return;
        if (replicated.srcNode().equals(nodeIdProvider.nodeId())) return;
        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        replicaCount.lock(bookId);
        try {
            int current = replicaCount.getOrDefault(bookId, 0);
            if (current < replicationFactor) {

                boolean savedToDisk = saveRetrievedBook(bookId, replicated.header(), replicated.body());

                if (savedToDisk) logger.info("Book {} materialized to disk and RAM.", bookId);
                else logger.warn("PERMISSIONS FAIL: Book {} kept in RAM ONLY.", bookId);

                replicaCount.put(bookId, current + 1);
            }
        } finally {replicaCount.unlock(bookId);}
    }

    public boolean saveRetrievedBook(int bookId, String header, String body) {
        try {
            DateTimePathProvider dateTimePathProvider = new DateTimePathProvider(this.datalakeBasePath);
            Path path = dateTimePathProvider.provide();
            if (!Files.exists(path)) Files.createDirectories(path);
            Path headerPath = path.resolve(bookId + "_header.txt");
            Path contentPath = path.resolve(bookId + "_body.txt");
            Files.writeString(headerPath, header);
            Files.writeString(contentPath, body);
            return true;
        } catch (Exception e) {
            logger.error("Disk write failed for book {}. Reason: {}", bookId, e.getMessage());
            return false;
        }
    }
}
