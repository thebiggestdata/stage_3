package com.thebiggestdata.search.infrastructure.adapters.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.search.model.BookMetadata;
import com.thebiggestdata.search.model.IndexGeneration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HazelcastSearchAdaptersTest {

    private static HazelcastInstance hazelcast;

    @BeforeAll
    static void startHazelcast() {
        Config config = new Config().setClusterName("search-test-" + UUID.randomUUID());
        config.setProperty("hazelcast.logging.type", "none");
        config.getNetworkConfig().setPort(15921).setPortAutoIncrement(true);
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.getSerializationConfig().getCompactSerializationConfig()
                .addSerializer(new BookMetadataSerializer());
        config.addMapConfig(new MapConfig(HazelcastNames.INVERTED_INDEX + ":*"));
        hazelcast = Hazelcast.newHazelcastInstance(config);
    }

    @AfterAll
    static void stopHazelcast() {
        hazelcast.shutdown();
    }

    @Test
    void readsPostingFrequenciesAndMetadataFromTheRequestedGeneration() {
        IndexGeneration generation = new IndexGeneration("v2");
        hazelcast.<String, Set<String>>getMap("inverted-index:v2").put("clean", Set.of("42:3"));
        hazelcast.<Integer, BookMetadata>getMap("bookMetadata:v2").put(
                42,
                new BookMetadata("Clean Code", "Robert Martin", "English", 2008)
        );

        assertEquals(Map.of(42, 3), new HazelcastIndexStore(hazelcast).find(generation, "clean"));
        assertEquals(
                "Clean Code",
                new HazelcastMetadataStore(hazelcast).findAll(generation, Set.of(42)).get(42).title()
        );
    }
}
