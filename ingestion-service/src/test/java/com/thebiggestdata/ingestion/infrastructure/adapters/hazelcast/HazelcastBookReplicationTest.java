package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookStorage;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.ReplicationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazelcastBookReplicationTest {

    private final List<HazelcastInstance> instances = new ArrayList<>();

    @AfterEach
    void stopHazelcast() {
        instances.forEach(HazelcastInstance::shutdown);
        instances.clear();
    }

    @Test
    void usesOnlyLocalReplicaWhenThereIsNoIngestionPeer() {
        int port = 16200 + ThreadLocalRandom.current().nextInt(200);
        HazelcastInstance node = startNode("single-replication-" + UUID.randomUUID(), "node-a", port, port);
        Book book = new Book(42, new BookContent("header", "body"));

        ReplicationResult result = new HazelcastBookReplicator(
                node,
                "node-a",
                2,
                Duration.ofSeconds(1),
                Duration.ofMillis(10)
        ).replicate(book);

        assertEquals(1, result.requiredReplicas());
        assertEquals(List.of("node-a"), result.replicaNodeIds());
        assertTrue(node.<Integer>getQueue(HazelcastNames.replicationQueueFor("node-a")).isEmpty());
    }

    @Test
    void sendsReplicationWorkToAnActivePeerQueue() throws Exception {
        int basePort = 16400 + ThreadLocalRandom.current().nextInt(200);
        String clusterName = "directed-replication-" + UUID.randomUUID();
        HazelcastInstance source = startNode(clusterName, "node-a", basePort, basePort);
        HazelcastInstance target = startNode(clusterName, "node-b", basePort + 1, basePort);
        waitForClusterSize(source, 2);

        Book book = new Book(77, new BookContent("header", "body"));
        new HazelcastDatalake(source).save(book);

        RecordingBookStorage storage = new RecordingBookStorage();
        try (HazelcastReplicationWorker worker = new HazelcastReplicationWorker(
                target,
                "node-b",
                new HazelcastDatalake(target),
                storage
        )) {
            worker.start();

            ReplicationResult result = new HazelcastBookReplicator(
                    source,
                    "node-a",
                    2,
                    Duration.ofSeconds(3),
                    Duration.ofMillis(10)
            ).replicate(book);

            assertEquals(2, result.requiredReplicas());
            assertEquals(List.of("node-a", "node-b"), result.replicaNodeIds());
            assertTrue(storage.contains(77));
            assertTrue(source.<Integer>getQueue(HazelcastNames.replicationQueueFor("node-a")).isEmpty());
        }
    }

    private HazelcastInstance startNode(
            String clusterName,
            String nodeId,
            int port,
            int seedPort
    ) {
        Config config = new Config().setClusterName(clusterName);
        config.setProperty("hazelcast.logging.type", "none");
        config.setProperty("hazelcast.wait.seconds.before.join", "0");
        config.getMemberAttributeConfig()
                .setAttribute("role", "ingestion")
                .setAttribute("nodeId", nodeId);
        config.getSerializationConfig()
                .getCompactSerializationConfig()
                .addSerializer(new BookContentSerializer());
        config.addMapConfig(new MapConfig(HazelcastNames.DATALAKE));
        config.addMapConfig(new MapConfig(HazelcastNames.REPLICATED_NODES));
        config.addQueueConfig(new QueueConfig(HazelcastNames.REPLICATION_QUEUE + ":*"));
        config.getNetworkConfig().setPort(port).setPortAutoIncrement(false);

        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getAutoDetectionConfig().setEnabled(false);
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).addMember("127.0.0.1:" + seedPort);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        instances.add(instance);
        return instance;
    }

    private void waitForClusterSize(HazelcastInstance instance, int size) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (instance.getCluster().getMembers().size() == size) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
        assertEquals(size, instance.getCluster().getMembers().size());
    }

    private static final class RecordingBookStorage implements BookStorage {
        private final Map<Integer, Book> books = new ConcurrentHashMap<>();

        @Override
        public void save(Book book) {
            books.put(book.bookId(), book);
        }

        private boolean contains(int bookId) {
            return books.containsKey(bookId);
        }
    }
}
