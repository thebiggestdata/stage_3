package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastConfig {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);
        NetworkConfig networkConfig = config.getNetworkConfig();
        String portStr = System.getenv().getOrDefault("HZ_NETWORK_PORT", "5701");
        int port = Integer.parseInt(portStr);
        networkConfig.setPort(port);
        String autoIncrement = System.getenv().getOrDefault("HZ_NETWORK_PORT_AUTO_INCREMENT", "false");
        networkConfig.setPortAutoIncrement(Boolean.parseBoolean(autoIncrement));
        config.setProperty("hazelcast.wait.seconds.before.join", "0");
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "10");
        config.setProperty("hazelcast.max.join.seconds", "20");
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAutoDetectionConfig().setEnabled(false);
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        String membersEnv = System.getenv("HZ_MEMBERS");
        if (membersEnv != null && !membersEnv.isEmpty()) {
            String[] members = membersEnv.split(",");
            for (String member : members) {
                tcpIpConfig.addMember(member.trim());
                logger.info("Added Hazelcast member: {}", member.trim());
            }
        }
        MultiMapConfig datalakeConfig = new MultiMapConfig("datalake");
        datalakeConfig.setBackupCount(2);
        datalakeConfig.setAsyncBackupCount(1);
        config.addMultiMapConfig(datalakeConfig);
        MapConfig replicationCountConfig = new MapConfig("replication-count");
        replicationCountConfig.setBackupCount(2);
        config.addMapConfig(replicationCountConfig);
        MapConfig lockMapConfig = new MapConfig("book-locks");
        lockMapConfig.setBackupCount(2);
        config.addMapConfig(lockMapConfig);
        logger.info("Starting Hazelcast member for cluster: {}", clusterName);
        logger.info("Listening on port: {}", port);
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        logger.info("Hazelcast member started successfully");
        logger.info("Cluster members: {}", instance.getCluster().getMembers());
        return instance;
    }
}