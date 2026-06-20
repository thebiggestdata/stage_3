package com.thebiggestdata.infrastructure.adapter.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.infrastructure.adapter.filesystem.BookArchiveByDate;
import com.thebiggestdata.infrastructure.config.ClusterConfig;
import com.thebiggestdata.domain.gateway.BookSource;
import com.thebiggestdata.domain.entity.BookText;
import com.thebiggestdata.domain.entity.NodeDetails;

public class ClusterManager {

    HazelcastInstance hazelcastInstance;
    NodeDetails nodeInformation;
    HazelcastDatalakeListener hazelcastDatalakeListener;
    HazelcastReplicationRunner hazelcastReplicationExecuter;

    public ClusterManager(String clusterName, int replicationFactor, BookSource bookProvider, BookArchiveByDate bookStorageDate) {
        this.nodeInformation = new NodeDetails(System.getenv("HZ_PUBLIC_ADDRESS"));
        this.hazelcastInstance = new ClusterConfig().initHazelcast(clusterName);

        this.hazelcastDatalakeListener = new HazelcastDatalakeListener(this.hazelcastInstance,
                this.nodeInformation, bookProvider, bookStorageDate);

        this.hazelcastDatalakeListener.registerListener();

        this.hazelcastReplicationExecuter = new HazelcastReplicationRunner(this.hazelcastInstance,
                this.nodeInformation, replicationFactor);
    }

    public void uploadBookToMemory(int bookId, String[] contentSeparated) {
        String header = contentSeparated[0];
        String body = contentSeparated[1];
        IMap<Integer, BookText> datalake = this.hazelcastInstance.getMap("datalake");
        datalake.put(bookId, new BookText(header, body));
    }

    public HazelcastInstance getHazelcastInstance() {
        return this.hazelcastInstance;
    }

    public HazelcastReplicationRunner getHazelcastReplicationExecuter() {
        return hazelcastReplicationExecuter;
    }
}