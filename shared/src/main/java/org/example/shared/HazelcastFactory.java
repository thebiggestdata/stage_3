package org.example.shared;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MultiMapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Factory that creates (or retrieves) a Hazelcast member instance with
 * multicast discovery enabled — suitable for a Windows university lab
 * where IP addresses may change between sessions.
 */
public final class HazelcastFactory {

    private HazelcastFactory() {}

    public static HazelcastInstance createInstance() {
        Config config = new Config();
        config.setClusterName(Constants.CLUSTER_NAME);

        // Network configuration
        NetworkConfig network = config.getNetworkConfig();
        network.setPort(5701);
        network.setPortAutoIncrement(true);

        // Use multicast discovery so nodes find each other automatically
        JoinConfig join = network.getJoin();
        join.getTcpIpConfig().setEnabled(false);
        join.getMulticastConfig().setEnabled(true);
        join.getMulticastConfig().setMulticastGroup("224.2.2.3");
        join.getMulticastConfig().setMulticastPort(54327);

        // Configure the distributed inverted-index MultiMap
        MultiMapConfig multiMapConfig = new MultiMapConfig();
        multiMapConfig.setName(Constants.INVERTED_INDEX_MAP);
        multiMapConfig.setValueCollectionType(MultiMapConfig.ValueCollectionType.SET);
        config.addMultiMapConfig(multiMapConfig);

        return Hazelcast.newHazelcastInstance(config);
    }
}
