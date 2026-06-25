package com.thebiggestdata.indexing.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingConfigurationTest {

    @Test
    void keepsUnrecoverableDatalakeMissAcknowledgementDisabledByDefault() {
        IndexingConfiguration configuration = IndexingConfiguration.load(new String[0], Map.of());

        assertFalse(configuration.acknowledgeUnrecoverableDatalakeMiss());
    }

    @Test
    void parsesUnrecoverableDatalakeMissAcknowledgementFlag() {
        IndexingConfiguration configuration = IndexingConfiguration.load(
                new String[0],
                Map.of("ACK_UNRECOVERABLE_DATALAKE_MISS", "true")
        );

        assertTrue(configuration.acknowledgeUnrecoverableDatalakeMiss());
    }
}
