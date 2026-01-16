package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast configuration that creates a MEMBER node (not a client).
 * Each service becomes part of the distributed cluster.
 */
public class HazelcastConfig {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        // Network configuration
        NetworkConfig networkConfig = config.getNetworkConfig();
        
        int port = Integer.parseInt(System.getenv().getOrDefault("HZ_PORT", "5701"));
        networkConfig.setPort(port);
        networkConfig.setPortAutoIncrement(false);
        
        // Public address for cross-machine communication
        String publicAddress = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddress != null && !publicAddress.isBlank()) {
            networkConfig.setPublicAddress(publicAddress);
            logger.info("Hazelcast public address: {}", publicAddress);
        }

        // Join configuration - TCP/IP only (no multicast)
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAutoDetectionConfig().setEnabled(false);
        
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        
        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            for (String member : members.split(",")) {
                String trimmed = member.trim();
                if (!trimmed.isEmpty()) {
                    tcpIpConfig.addMember(trimmed);
                    logger.info("Added Hazelcast member: {}", trimmed);
                }
            }
        }

        // Faster cluster formation
        config.setProperty("hazelcast.wait.seconds.before.join", "0");
        config.setProperty("hazelcast.max.join.seconds", "10");

        // Map configurations with replication
        config.addMapConfig(new MapConfig("datalake")
                .setBackupCount(2)
                .setAsyncBackupCount(0));

        config.addMapConfig(new MapConfig("inverted-index")
                .setBackupCount(2)
                .setAsyncBackupCount(0));

        config.addMapConfig(new MapConfig("book-metadata")
                .setBackupCount(2)
                .setAsyncBackupCount(0));

        config.addMapConfig(new MapConfig("download-log")
                .setBackupCount(2)
                .setAsyncBackupCount(0));

        config.addMapConfig(new MapConfig("processed-documents")
                .setBackupCount(2)
                .setAsyncBackupCount(0));

        logger.info("Creating Hazelcast MEMBER for cluster '{}'", clusterName);
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        
        logger.info("Hazelcast MEMBER started successfully");
        logger.info("Cluster size: {}", instance.getCluster().getMembers().size());
        logger.info("Member UUID: {}", instance.getLocalEndpoint().getUuid());

        return instance;
    }
}