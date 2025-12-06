package com.searchengine.indexer.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastConfig {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);
    private final HazelcastInstance hazelcastInstance;

    public HazelcastConfig(String clusterName) {
        logger.info("Connecting to Hazelcast cluster: {}", clusterName);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(clusterName);
        this.hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
        logger.info("Connected to Hazelcast cluster successfully");
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }
}

