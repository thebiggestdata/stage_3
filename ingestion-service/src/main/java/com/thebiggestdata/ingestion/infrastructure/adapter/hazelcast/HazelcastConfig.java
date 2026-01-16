package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastConfig {

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        String members = System.getenv().getOrDefault("HZ_MEMBERS", "localhost:5701");
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true);
        for (String member : members.split(",")) {
            joinConfig.getTcpIpConfig().addMember(member.trim());
        }

        return Hazelcast.newHazelcastInstance(config);
    }
}
