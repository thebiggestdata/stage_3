package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.model.NodeIdProvider;

public class HazelcastDuplicationManager {
    HazelcastInstance hazelcastInstance;
    NodeIdProvider nodeIdProvider;
    HazelcastDatalakeListener hazelcastDatalakeListener;
    HazelcastDuplicationProvider hazelcastDuplicationProvider;

    public HazelcastDuplicationManager(String clusterName, int replicationFactor) {
        this.nodeIdProvider = new NodeIdProvider(System.getenv("PUBLIC_IP"));
        this.hazelcastInstance = new HazelcastConfig().initHazelcast(clusterName);
        this.hazelcastDatalakeListener = new HazelcastDatalakeListener(this.hazelcastInstance,this.nodeIdProvider,replicationFactor);
        this.hazelcastDatalakeListener.registerListener();
        this.hazelcastDuplicationProvider = new HazelcastDuplicationProvider(this.hazelcastInstance, this.nodeIdProvider);
    }

    public HazelcastInstance getHazelcastInstance() {return this.hazelcastInstance;}

    public NodeIdProvider getNodeInfoProvider() {return this.nodeIdProvider;}

    public HazelcastDuplicationProvider getHazelcastDuplicationProvider() {return hazelcastDuplicationProvider;}
}
