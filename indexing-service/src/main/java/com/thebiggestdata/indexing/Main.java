package com.thebiggestdata.indexing;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexing.application.usecases.ExecuteRebuildUseCase;
import com.thebiggestdata.indexing.application.usecases.HandleBookIngestedUseCase;
import com.thebiggestdata.indexing.application.usecases.IndexBookUseCase;
import com.thebiggestdata.indexing.application.usecases.RebuildIndexUseCase;
import com.thebiggestdata.indexing.application.usecases.RecoverIndexUseCase;
import com.thebiggestdata.indexing.application.usecases.TermFrequencyAnalyzer;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.ActiveMQIndexingEventConsumer;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.ActiveMQIngestionControlPublisher;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.ActiveMQRebuildCommandConsumer;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.ActiveMQRebuildCommandPublisher;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.BookIngestedMessageMapper;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.JmsTopicPublisher;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.RebuildCommandMessageMapper;
import com.thebiggestdata.indexing.infrastructure.adapters.filesystem.FilesystemBookArchive;
import com.thebiggestdata.indexing.infrastructure.adapters.filesystem.FilesystemBookContentArchive;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastBookContentStore;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastClusterTopology;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastIndexGenerationStore;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastIndexingTracker;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastInvertedIndex;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastMetadataStore;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastPendingBookSeeder;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastRebuildCoordination;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastRebuildState;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastTokenMetrics;
import com.thebiggestdata.indexing.infrastructure.adapters.metadata.GutenbergMetadataExtractor;
import com.thebiggestdata.indexing.infrastructure.adapters.recovery.RecoveringBookContentStore;
import com.thebiggestdata.indexing.infrastructure.adapters.tokenizer.JsonStopWordsLoader;
import com.thebiggestdata.indexing.infrastructure.adapters.tokenizer.TextTokenizer;
import com.thebiggestdata.indexing.infrastructure.adapters.web.IndexingController;
import com.thebiggestdata.indexing.infrastructure.config.ActiveMQConfig;
import com.thebiggestdata.indexing.infrastructure.config.HazelcastConfig;
import com.thebiggestdata.indexing.infrastructure.config.IndexingConfiguration;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentStore;
import com.thebiggestdata.indexing.model.IndexGeneration;
import com.thebiggestdata.indexing.model.RecoveryResult;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] arguments) {
        IndexingConfiguration configuration = IndexingConfiguration.load(arguments, System.getenv());
        Gson gson = new Gson();
        HazelcastInstance hazelcast = new HazelcastConfig().start(configuration.hazelcastClusterName());

        HazelcastClusterTopology topology = new HazelcastClusterTopology(hazelcast);
        String localNodeId = topology.localNodeId();
        HazelcastIndexGenerationStore generations = new HazelcastIndexGenerationStore(hazelcast);
        HazelcastBookContentStore liveBooks = new HazelcastBookContentStore(hazelcast);
        BookContentStore books = new RecoveringBookContentStore(
                liveBooks,
                new FilesystemBookContentArchive(configuration.datalakeRoot())
        );
        HazelcastInvertedIndex index = new HazelcastInvertedIndex(hazelcast, configuration.indexWriters());
        HazelcastIndexingTracker tracker = new HazelcastIndexingTracker(
                hazelcast,
                localNodeId,
                configuration.indexingClaimLease()
        );
        HazelcastMetadataStore metadata = new HazelcastMetadataStore(hazelcast);
        HazelcastTokenMetrics tokenMetrics = new HazelcastTokenMetrics(hazelcast);
        HazelcastPendingBookSeeder pendingBooks = new HazelcastPendingBookSeeder(
                hazelcast,
                localNodeId,
                configuration.lastBookId()
        );

        IndexBookUseCase indexBook = new IndexBookUseCase(
                books,
                index,
                tracker,
                generations,
                metadata,
                tokenMetrics,
                new TermFrequencyAnalyzer(new TextTokenizer(new JsonStopWordsLoader().load())),
                new GutenbergMetadataExtractor(),
                localNodeId
        );
        RecoverIndexUseCase recoverIndex = new RecoverIndexUseCase(
                new FilesystemBookArchive(configuration.datalakeRoot()),
                books,
                indexBook
        );

        IndexGeneration activeGeneration = generations.active();
        RecoveryResult startupRecovery = recoverIndex.execute(activeGeneration);
        pendingBooks.seedAfter(startupRecovery.maxBookId());

        HazelcastRebuildCoordination coordination = new HazelcastRebuildCoordination(
                hazelcast,
                configuration.rebuildTimeout().plusSeconds(60)
        );
        HazelcastRebuildState rebuildState = new HazelcastRebuildState(hazelcast);
        ActiveMQConnectionFactory jmsFactory = new ActiveMQConfig().create(
                configuration.brokerUrl(),
                configuration.eventPrefetch(),
                configuration.maxRedeliveries()
        );
        JmsTopicPublisher topicPublisher = new JmsTopicPublisher(jmsFactory);
        RebuildCommandMessageMapper rebuildMapper = new RebuildCommandMessageMapper(gson);

        RebuildIndexUseCase rebuildIndex = new RebuildIndexUseCase(
                new ActiveMQIngestionControlPublisher(topicPublisher, gson),
                index,
                tracker,
                generations,
                metadata,
                tokenMetrics,
                pendingBooks,
                new ActiveMQRebuildCommandPublisher(topicPublisher, rebuildMapper),
                coordination,
                topology,
                configuration.rebuildTimeout()
        );
        ExecuteRebuildUseCase executeRebuild = new ExecuteRebuildUseCase(
                recoverIndex,
                coordination,
                topology
        );
        HandleBookIngestedUseCase handleBookIngested = new HandleBookIngestedUseCase(
                indexBook,
                rebuildState,
                configuration.inProgressRetryTimeout(),
                configuration.inProgressRetryDelay()
        );

        ActiveMQRebuildCommandConsumer rebuildConsumer = new ActiveMQRebuildCommandConsumer(
                jmsFactory,
                rebuildMapper,
                localNodeId
        );
        ActiveMQIndexingEventConsumer eventConsumer = new ActiveMQIndexingEventConsumer(
                jmsFactory,
                new BookIngestedMessageMapper(gson),
                configuration.eventConsumers(),
                localNodeId,
                exception -> configuration.acknowledgeUnrecoverableDatalakeMiss()
                        && exception instanceof HandleBookIngestedUseCase.IndexingNotCompletedException indexing
                        && indexing.result().isUnrecoverableMissingContentFailure()
        );
        log.info(
                "INDEXING_SERVICE_STARTED nodeId={} brokerUrl={} consumers={} prefetch={} maxRedeliveries={} indexWriters={} ackUnrecoverableDatalakeMiss={} hazelcastCluster={}",
                localNodeId,
                configuration.brokerUrl(),
                configuration.eventConsumers(),
                configuration.eventPrefetch(),
                configuration.maxRedeliveries(),
                configuration.indexWriters(),
                configuration.acknowledgeUnrecoverableDatalakeMiss(),
                configuration.hazelcastClusterName()
        );
        rebuildConsumer.start(executeRebuild::execute);
        eventConsumer.start(handleBookIngested::execute);

        Javalin application = httpApplication(
                new IndexingController(indexBook, rebuildIndex, gson),
                configuration.servicePort(),
                gson
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            application.stop();
            eventConsumer.close();
            rebuildConsumer.close();
            topicPublisher.close();
            pendingBooks.close();
            index.close();
            hazelcast.shutdown();
        }, "indexing-shutdown"));
    }

    private static Javalin httpApplication(IndexingController controller, int port, Gson gson) {
        Javalin application = Javalin.create(config ->
                config.http.defaultContentType = "application/json"
        );
        application.exception(IllegalArgumentException.class, (exception, context) -> {
            context.status(400);
            context.result(gson.toJson(Map.of("error", exception.getMessage())));
        });
        application.post("/index/books/{book_id}", controller::index);
        application.post("/index/rebuild", controller::rebuild);
        application.get("/health", context -> context.result("{\"status\":\"UP\"}"));
        return application.start(port);
    }
}
