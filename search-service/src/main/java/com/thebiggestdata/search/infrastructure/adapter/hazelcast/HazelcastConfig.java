// HazelcastConfig.java
package com.thebiggestdata.search.infrastructure.adapter.hazelcast;

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

        // Network configuration
        NetworkConfig networkConfig = config.getNetworkConfig();

        String portStr = System.getenv().getOrDefault("HZ_NETWORK_PORT", "5701");
        int port = Integer.parseInt(portStr);
        networkConfig.setPort(port);

        String autoIncrement = System.getenv().getOrDefault("HZ_NETWORK_PORT_AUTO_INCREMENT", "true");
        networkConfig.setPortAutoIncrement(Boolean.parseBoolean(autoIncrement));

        // Reduce join time
        config.setProperty("hazelcast.wait.seconds.before.join", "0");
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "10");
        config.setProperty("hazelcast.max.join.seconds", "20");

        // Join configuration
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

        logger.info("Starting Hazelcast member for cluster: {}", clusterName);
        logger.info("Listening on port: {}", port);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        logger.info("Hazelcast member started successfully");
        logger.info("Cluster members: {}", instance.getCluster().getMembers());

        return instance;
    }
}
