package com.thebiggestdata.ingestion.infrastructure.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.SetConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.BookContentSerializer;
import com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast.HazelcastNames;

public final class HazelcastConfig {

    private static final String ROLE = "role";
    private static final String INGESTION = "ingestion";
    private static final String NODE_ID = "nodeId";

    public HazelcastInstance start(String clusterName) {

        Config config = new Config();
        config.setClusterName(clusterName);
        config.getMemberAttributeConfig().setAttribute(ROLE, INGESTION);
        String nodeId = stableNodeId();
        if (nodeId != null && !nodeId.isBlank()) {
            config.getMemberAttributeConfig().setAttribute(NODE_ID, nodeId);
        }

        config.getSerializationConfig()
                .getCompactSerializationConfig()
                .addSerializer(new BookContentSerializer());

        setMapConfig(config);
        setNetworkConfig(config);
        setJoinConfig(config);

        return Hazelcast.newHazelcastInstance(config);
    }

    private void setMapConfig(Config config) {
        int backupCount = environmentInt("HAZELCAST_BACKUP_COUNT", 2);
        config.addMapConfig(new MapConfig(HazelcastNames.DATALAKE).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.INGESTIONS_IN_PROGRESS).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.REPLICATED_NODES).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.INGESTION_ATTEMPTS).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.FAILED_INGESTIONS).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.INDEX_GENERATIONS).setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.BOOK_METADATA + ":*").setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.INDEXING_IN_PROGRESS + ":*").setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.TOKEN_COUNTS + ":*").setBackupCount(backupCount));
        config.addMapConfig(new MapConfig(HazelcastNames.INVERTED_INDEX + ":*")
                .setBackupCount(backupCount)
                .setAsyncBackupCount(0));
        config.addQueueConfig(new QueueConfig(HazelcastNames.PENDING_BOOKS).setBackupCount(backupCount));
        config.addQueueConfig(new QueueConfig(HazelcastNames.REPLICATION_QUEUE + ":*").setBackupCount(backupCount));
        config.addSetConfig(new SetConfig(HazelcastNames.DOWNLOADED_BOOKS).setBackupCount(backupCount));
        config.addSetConfig(new SetConfig(HazelcastNames.INDEXED_BOOKS + ":*").setBackupCount(backupCount));
    }

    private void setNetworkConfig(Config config) {
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(environmentInt("HZ_PORT", 5701));
        networkConfig.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        String publicAddr = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddr != null && !publicAddr.isBlank()) {
            networkConfig.setPublicAddress(publicAddr);
        }
    }

    private void setJoinConfig(Config config) {
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getAutoDetectionConfig().setEnabled(false);

        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            join.getMulticastConfig().setEnabled(false);
            join.getTcpIpConfig().setEnabled(true);
            for (String m : members.split(",")) {
                join.getTcpIpConfig().addMember(m.trim());
            }
        } else {
            join.getTcpIpConfig().setEnabled(false);
            join.getMulticastConfig().setEnabled(true);
        }
    }

    private int environmentInt(String name, int defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private String stableNodeId() {
        String nodeId = System.getenv("HZ_NODE_ID");
        if (nodeId != null && !nodeId.isBlank()) {
            return nodeId;
        }
        return System.getenv("HZ_PUBLIC_ADDRESS");
    }
}

