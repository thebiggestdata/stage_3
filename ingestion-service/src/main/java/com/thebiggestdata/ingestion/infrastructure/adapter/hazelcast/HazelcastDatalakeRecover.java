package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.activemq.ActiveMQIngestedBookProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HazelcastDatalakeRecover {
    private final HazelcastInstance hazelcast;
    private final NodeIdProvider nodeInfoProvider;
    private final ActiveMQIngestedBookProvider notifier;

    public HazelcastDatalakeRecover(HazelcastInstance hazelcast,
                                    NodeIdProvider nodeInfoProvider,
                                    ActiveMQIngestedBookProvider notifier) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.notifier = notifier;
    }

    public void reloadMemoryFromDisk(String dataVolumePath) throws IOException {
        Path datalakePath = Paths.get(dataVolumePath);
        MultiMap<Integer, DuplicatedBook> datalake = hazelcast.getMultiMap("datalake");
        IMap<Integer, Boolean> lockMap = hazelcast.getMap("book-locks");

        if (!Files.exists(datalakePath) || !Files.isDirectory(datalakePath)) {
            throw new IOException("Datalake path doesn't exist: " + dataVolumePath);
        }

        Files.walk(datalakePath)
                .filter(path -> path.getFileName().toString().endsWith("_body.txt"))
                .forEach(bodyPath -> {
                    try {
                        int bookId = extractId(bodyPath.getFileName().toString());
                        if (datalake.containsKey(bookId)) {
                            System.out.println("Skipping Book " + bookId + ", already in the in-memory datalake.");
                            return;
                        }
                        lockMap.lock(bookId);
                        try {
                            Path headerPath = bodyPath.getParent().resolve(bookId + "_header.txt");
                            String header = Files.readString(headerPath);
                            String body = Files.readString(bodyPath);
                            datalake.put(bookId, new DuplicatedBook(header, body, nodeInfoProvider.nodeId()));
                        } finally {
                            lockMap.unlock(bookId);
                        }
                        notifier.provide(bookId);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading from disk: " + bodyPath, e);
                    }
                });
    }

    private int extractId(String filename) {
        String suffix = "_body.txt";
        int index = filename.indexOf(suffix);
        String idStr = filename.substring(0, index);
        return Integer.parseInt(idStr);
    }
}