package com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.thebiggestdata.ingestion.model.DuplicatedBook;
import com.thebiggestdata.ingestion.model.NodeIdProvider;
import com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider.DateTimePathProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HazelcastDatalakeListener extends HzlcstEntryListener<Integer, DuplicatedBook> {
    private final NodeIdProvider nodeIdProvider;
    private final int replicationFactor;
    private final HazelcastInstance hazelcast;

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
        if (replicated == null || replicated.srcNode() == null) {
            System.out.println("Skipping event for book " + bookId + ": srcNode is null");
            return;
        }
        if (replicated.srcNode().equals(nodeIdProvider.nodeId())) return;
        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        int current = replicaCount.getOrDefault(bookId, 1);
        if (current >= replicationFactor) return;
        replicaCount.put(bookId, current + 1);
        saveRetrievedBook(bookId, replicated.header(), replicated.body());
    }

    public void saveRetrievedBook(int bookId, String header, String body) {
        try {
            DateTimePathProvider dateTimePathProvider = new DateTimePathProvider("datalake");
            Path path = dateTimePathProvider.provide();
            Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
            Path contentPath = path.resolve(String.format("%d_body.txt", bookId));
            Files.writeString(headerPath, header);
            Files.writeString(contentPath, body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
