package com.thebiggestdata.ingestion.application;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.model.NodeIdProvider;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DocIngestionExecutor {
    private final HazelcastInstance hazelcast;
    private final DownloadDocumentProvider ingestBookService;
    private final NodeIdProvider nodeIdProvider;
    private IQueue<Integer> queue;

    public DocIngestionExecutor(HazelcastInstance hazelcast, DownloadDocumentProvider ingestBookService, NodeIdProvider nodeIdProvider) {
        this.hazelcast = hazelcast;
        this.ingestBookService = ingestBookService;
        this.nodeIdProvider = nodeIdProvider;
        this.queue = this.hazelcast.getQueue("books");
    }

    public void setupBookQueue() {
        int nodeIndex = extractNodeIndex(nodeIdProvider.nodeId());
        int totalNodes = hazelcast.getCluster().getMembers().size();
        System.out.println("Node " + nodeIndex + " of " + totalNodes + " nodes");
        for (int i = 1; i <= 100000; i++) if (i % totalNodes == nodeIndex) this.queue.add(i);
        System.out.println("Node " + nodeIndex + " queued " + queue.size() + " books");
    }

    private int extractNodeIndex(String nodeId) {
        String[] parts = nodeId.split("-");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    public void startPeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::execute, 0, 200, TimeUnit.MILLISECONDS);
    }

    public void execute() {
        try {
            Integer bookId = this.queue.take();
            System.out.println("\nIngesting book: " + bookId);
            Map<String, Object> result = ingestBookService.ingest(bookId);
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
