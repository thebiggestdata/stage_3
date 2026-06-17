package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.adapter.filesystem.BookStorageDate;
import com.thebiggestdata.ingestion.infrastructure.config.HazelcastConfig;
import com.thebiggestdata.ingestion.infrastructure.port.BookProvider;
import com.thebiggestdata.ingestion.model.BookContent;
import com.thebiggestdata.ingestion.model.NodeInformation;

public class HazelcastManager {

    HazelcastInstance hazelcastInstance;
    NodeInformation nodeInformation;
    HazelcastDatalakeListener hazelcastDatalakeListener;
    HazelcastReplicationExecuter hazelcastReplicationExecuter;

    public HazelcastManager(String clusterName, int replicationFactor, BookProvider bookProvider, BookStorageDate bookStorageDate) {
        this.nodeInformation = new NodeInformation(System.getenv("HZ_PUBLIC_ADDRESS"));
        this.hazelcastInstance = new HazelcastConfig().initHazelcast(clusterName);

        this.hazelcastDatalakeListener = new HazelcastDatalakeListener(this.hazelcastInstance,
                this.nodeInformation, bookProvider, bookStorageDate);

        this.hazelcastDatalakeListener.registerListener();

        this.hazelcastReplicationExecuter = new HazelcastReplicationExecuter(this.hazelcastInstance,
                this.nodeInformation, replicationFactor);
    }

    public void uploadBookToMemory(int bookId, String[] contentSeparated) {
        String header = contentSeparated[0];
        String body = contentSeparated[1];
        IMap<Integer, BookContent> datalake = this.hazelcastInstance.getMap("datalake");
        datalake.put(bookId, new BookContent(header, body));
    }

    public HazelcastInstance getHazelcastInstance() {
        return this.hazelcastInstance;
    }

    public HazelcastReplicationExecuter getHazelcastReplicationExecuter() {
        return hazelcastReplicationExecuter;
    }
}