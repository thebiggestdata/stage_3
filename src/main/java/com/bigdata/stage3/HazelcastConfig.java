package com.bigdata.stage3;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;

/**
 * Shared Hazelcast configuration for the distributed system.
 * Uses TCP/IP discovery without multicast, suitable for restricted environments.
 */
public class HazelcastConfig {
    
    public static final String CLUSTER_NAME = "stage3-cluster";
    public static final String TASK_QUEUE_NAME = "task-queue";
    public static final String RESULT_MAP_NAME = "result-map";
    
    /**
     * Creates a Hazelcast configuration for TCP/IP cluster.
     * 
     * @param members List of member IP addresses in the format "IP:PORT" (e.g., "127.0.0.1:5701")
     * @return Configured Hazelcast Config object
     */
    public static Config createConfig(String... members) {
        Config config = new Config();
        config.setClusterName(CLUSTER_NAME);
        
        // Configure network settings
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true);
        
        // Configure TCP/IP discovery
        JoinConfig joinConfig = networkConfig.getJoin();
        
        // Disable multicast (not needed in restricted environments)
        joinConfig.getMulticastConfig().setEnabled(false);
        
        // Enable TCP/IP with member list
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        
        // Add members to the cluster
        if (members != null && members.length > 0) {
            for (String member : members) {
                tcpIpConfig.addMember(member);
            }
        } else {
            // Default to localhost if no members specified
            tcpIpConfig.addMember("127.0.0.1");
        }
        
        // Disable AWS, Azure, GCP, Kubernetes discovery
        joinConfig.getAwsConfig().setEnabled(false);
        joinConfig.getAzureConfig().setEnabled(false);
        joinConfig.getGcpConfig().setEnabled(false);
        joinConfig.getKubernetesConfig().setEnabled(false);
        
        return config;
    }
    
    /**
     * Creates a default configuration for local testing (localhost only).
     */
    public static Config createLocalConfig() {
        return createConfig("127.0.0.1");
    }
}
