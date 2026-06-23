package com.thebiggestdata.indexing.infrastructure.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.SetConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.BookContentSerializer;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.BookMetadataSerializer;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastNames;

public final class HazelcastConfig {

    public HazelcastInstance start(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);
        config.getMemberAttributeConfig().setAttribute("role", "indexer");
        String nodeId = stableNodeId();
        if (nodeId != null && !nodeId.isBlank()) {
            config.getMemberAttributeConfig().setAttribute("nodeId", nodeId);
        }

        config.getSerializationConfig()
                .getCompactSerializationConfig()
                .addSerializer(new BookContentSerializer())
                .addSerializer(new BookMetadataSerializer());

        configureDataStructures(config);
        configureNetwork(config);
        configureDiscovery(config);
        return Hazelcast.newHazelcastInstance(config);
    }

    private void configureDataStructures(Config config) {
        int backups = environmentInt("HAZELCAST_BACKUP_COUNT", 1);
        config.addMapConfig(new MapConfig(HazelcastNames.INVERTED_INDEX + ":*")
                .setBackupCount(backups)
                .setAsyncBackupCount(0)
                .setStatisticsEnabled(true));
        config.addMapConfig(new MapConfig(HazelcastNames.DATALAKE).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.BOOK_METADATA + ":*").setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.INDEXING_IN_PROGRESS + ":*").setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.TOKEN_COUNTS + ":*").setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.INDEX_GENERATIONS).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.QUEUE_INITIALIZATION).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.REBUILD_STATE).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.REBUILD_EXPECTED).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.REBUILD_COMPLETIONS).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.REBUILD_MAX_BOOK_IDS).setBackupCount(backups));
        config.addMapConfig(new MapConfig(HazelcastNames.REBUILD_FAILURES).setBackupCount(backups));
        config.addSetConfig(new SetConfig(HazelcastNames.INDEXED_BOOKS + ":*").setBackupCount(backups));
        config.addQueueConfig(new QueueConfig(HazelcastNames.PENDING_BOOKS).setBackupCount(backups));
    }

    private void configureNetwork(Config config) {
        NetworkConfig network = config.getNetworkConfig();
        network.setPort(environmentInt("HZ_PORT", 5702));
        network.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        String publicAddress = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddress != null && !publicAddress.isBlank()) {
            network.setPublicAddress(publicAddress);
        }
    }

    private void configureDiscovery(Config config) {
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getAutoDetectionConfig().setEnabled(false);
        String members = System.getenv("HZ_MEMBERS");
        if (members == null || members.isBlank()) {
            join.getTcpIpConfig().setEnabled(false);
            join.getMulticastConfig().setEnabled(true);
            return;
        }

        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true);
        for (String member : members.split(",")) {
            join.getTcpIpConfig().addMember(member.trim());
        }
    }

    private int environmentInt(String name, int defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private String stableNodeId() {
        String nodeId = System.getenv("HZ_NODE_ID");
        return nodeId == null || nodeId.isBlank() ? System.getenv("HZ_PUBLIC_ADDRESS") : nodeId;
    }
}
