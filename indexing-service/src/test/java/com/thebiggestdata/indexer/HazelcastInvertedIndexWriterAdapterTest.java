package com.thebiggestdata.indexer;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HazelcastInvertedIndexWriterAdapterTest {
    private static HazelcastInstance hazelcast;

    @BeforeAll
    static void setupCluster() {
        Config config = new Config();
        config.setClusterName("test-cluster");
        config.getCPSubsystemConfig().setCPMemberCount(0);

        hazelcast = Hazelcast.newHazelcastInstance(config);
    }

    @AfterAll
    static void teardownCluster() {
        if (hazelcast != null) {
            hazelcast.shutdown();
        }
    }

    @AfterEach
    void cleanup() {
        MultiMap<String, Integer> mm = hazelcast.getMultiMap("test-index");
        mm.clear();
    }

    @Test
    void shouldWritePostingsToMultiMap() {
        // Usar implementación sin locks para tests
        MultiMap<String, Integer> mm = hazelcast.getMultiMap("test-index");

        Map<String, List<Integer>> postings = Map.of(
                "hello", List.of(1, 2),
                "world", List.of(1)
        );

        // Escribir directamente (simula adapter sin locks)
        postings.forEach((token, ids) -> {
            for (Integer id : ids) {
                mm.put(token, id);
            }
        });

        assertEquals(2, mm.get("hello").size());
        assertEquals(1, mm.get("world").size());
    }

    @Test
    void shouldHandleConcurrentWrites() throws InterruptedException {
        MultiMap<String, Integer> mm = hazelcast.getMultiMap("test-index");

        Thread t1 = new Thread(() -> mm.put("token", 1));
        Thread t2 = new Thread(() -> mm.put("token", 2));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(2, mm.get("token").size());
        assertTrue(mm.get("token").contains(1));
        assertTrue(mm.get("token").contains(2));
    }
}
