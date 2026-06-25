package com.thebiggestdata.ingestion.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionConfigurationTest {

    @Test
    void providesLocalDevelopmentDefaults() {
        IngestionConfiguration configuration = IngestionConfiguration.load(new String[0], Map.of());

        assertEquals(Path.of("datalake"), configuration.datalakeRoot());
        assertEquals("tcp://localhost:61616", configuration.brokerUrl());
        assertEquals(2, configuration.replicationFactor());
        assertEquals(20, configuration.indexingBufferFactor());
        assertEquals(4, configuration.ingestionWorkers());
        assertEquals(7001, configuration.servicePort());
    }

    @Test
    void readsEnvironmentAndDatalakeArgument() {
        IngestionConfiguration configuration = IngestionConfiguration.load(
                new String[]{"/data"},
                Map.of(
                        "REPLICATION_FACTOR", "3",
                        "INGESTION_WORKERS", "6",
                        "SERVICE_PORT", "9001"
                )
        );

        assertEquals(Path.of("/data"), configuration.datalakeRoot());
        assertEquals(3, configuration.replicationFactor());
        assertEquals(6, configuration.ingestionWorkers());
        assertEquals(9001, configuration.servicePort());
    }

    @Test
    void usesPublicHazelcastAddressAsStableNodeIdentity() {
        IngestionConfiguration configuration = IngestionConfiguration.load(
                new String[0],
                Map.of("HZ_PUBLIC_ADDRESS", "10.0.0.7:5701")
        );

        assertEquals("10.0.0.7:5701", configuration.nodeId().orElseThrow());
    }

    @Test
    void rejectsInvalidPositiveSettings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IngestionConfiguration.load(new String[0], Map.of("REPLICATION_FACTOR", "0"))
        );
    }
}
