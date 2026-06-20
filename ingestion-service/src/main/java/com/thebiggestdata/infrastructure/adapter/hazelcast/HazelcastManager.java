package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.infrastructure.adapter.filesystem.BookStorageDate;
import com.thebiggestdata.infrastructure.config.ClusterConfig;
import com.thebiggestdata.domain.gateway.BookSource;
import com.thebiggestdata.domain.entity.BookText;
import com.thebiggestdata.domain.entity.NodeDetails;

public class HazelcastManager {

    HazelcastInstance hazelcastInstance;
    NodeDetails nodeInformation;
    HazelcastDatalakeListener hazelcastDatalakeListener;
    HazelcastReplicationExecuter hazelcastReplicationExecuter;

    public HazelcastManager(String clusterName, int replicationFactor, BookSource bookProvider, BookStorageDate bookStorageDate) {
        this.nodeInformation = new NodeDetails(System.getenv("HZ_PUBLIC_ADDRESS"));
        this.hazelcastInstance = new ClusterConfig().initHazelcast(clusterName);

        this.hazelcastDatalakeListener = new HazelcastDatalakeListener(this.hazelcastInstance,
                this.nodeInformation, bookProvider, bookStorageDate);

        this.hazelcastDatalakeListener.registerListener();

        this.hazelcastReplicationExecuter = new HazelcastReplicationExecuter(this.hazelcastInstance,
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

    public HazelcastReplicationExecuter getHazelcastReplicationExecuter() {
        return hazelcastReplicationExecuter;
    }
}