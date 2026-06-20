package com.thebiggestdata.infrastructure.adapter.recovery;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReindexingRunnerImpl {
    private static final Logger log = LoggerFactory.getLogger(ReindexingRunnerImpl.class);

    private final InvertedIndexRestorer invertedIndexRecovery;
    private final HazelcastInstance hz;
    private final IngestionQueueCoordinator ingestionQueueManager;

    public ReindexingRunnerImpl(InvertedIndexRestorer invertedIndexRecovery, HazelcastInstance hz, IngestionQueueCoordinator ingestionQueueManager) {
        this.invertedIndexRecovery = invertedIndexRecovery;
        this.hz = hz;
        this.ingestionQueueManager = ingestionQueueManager;
    }

    public void executeRecovery(){
        int startReference = this.invertedIndexRecovery.executeRecovery();
        this.ingestionQueueManager.setupBookQueue(startReference);
    }

    public void rebuildIndex() {
        this.ingestionQueueManager.stopPopulation();

        this.hz.getSet("log").clear();
        this.hz.getSet("indexingRegistry").clear();
        this.hz.getMap("inverted-index").clear();
        this.hz.getMap("bookMetadata").clear();
        this.hz.getQueue("books").clear();

        this.hz.getCPSubsystem().getAtomicLong("queueInitialized").set(0);

        this.executeRecovery();

        log.info("Rebuild completed");
    }
}