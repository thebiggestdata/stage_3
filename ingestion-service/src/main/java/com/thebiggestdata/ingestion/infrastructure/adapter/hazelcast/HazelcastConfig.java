package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastConfig {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);

    public HazelcastInstance initHazelcast(String clusterName) {
        ClientConfig config = new ClientConfig();
        config.setClusterName(clusterName);

        String membersEnv = System.getenv("HZ_MEMBERS");
        if (membersEnv != null && !membersEnv.isEmpty()) {
            String[] members = membersEnv.split(",");
            config.getNetworkConfig().addAddress(members);
            logger.info("Configured Hazelcast members addresses: {}", (Object) members);
        } else {
            config.getNetworkConfig().addAddress("hazelcast1", "hazelcast2", "hazelcast3");
            logger.warn("HZ_MEMBERS not set. Using defaults: hazelcast1, hazelcast2, hazelcast3");
        }

        config.getNetworkConfig().setSmartRouting(true);
        config.getNetworkConfig().setRedoOperation(true);

        logger.info("Connecting to Hazelcast cluster '{}' as CLIENT...", clusterName);

        HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);

        logger.info("Hazelcast Client connected successfully");
        logger.info("Cluster members view: {}", instance.getCluster().getMembers());

        return instance;
    }
}