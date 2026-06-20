package com.thebiggestdata;

import com.thebiggestdata.infrastructure.adapter.web.IndexingEndpoint;
import com.thebiggestdata.infrastructure.adapter.recovery.CoordinateReindex;
import com.thebiggestdata.infrastructure.adapter.tokenizer.JsonStopWordsProvider;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastBookRepository;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQEventListener;
import com.thebiggestdata.infrastructure.adapter.activemq.ReindexMessageListener;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIndexRepository;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastMetadataRepository;
import com.thebiggestdata.infrastructure.adapter.hazelcast.MetadataReader;
import com.thebiggestdata.infrastructure.adapter.recovery.IngestionQueueCoordinator;
import com.thebiggestdata.infrastructure.adapter.recovery.InvertedIndexRestorer;
import com.thebiggestdata.infrastructure.adapter.recovery.ReindexingRunnerImpl;
import com.thebiggestdata.infrastructure.adapter.tokenizer.WhitespaceTokenizer;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIndexingStatusRepository;
import com.thebiggestdata.usecase.IndexBookUseCase;
import com.thebiggestdata.usecase.TermFrequencyCalculator;
import com.thebiggestdata.infrastructure.config.ClusterConfig;
import com.thebiggestdata.domain.gateway.EventListener;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String clusterName = System.getenv().getOrDefault("HAZELCAST_CLUSTER_NAME", "SearchEngine");

        ClusterConfig hazelcastConfig = new ClusterConfig();
        HazelcastInstance hz = hazelcastConfig.initHazelcast(clusterName);

        HazelcastIndexRepository indexStore = new HazelcastIndexRepository(hz);
        HazelcastBookRepository bookStore = new HazelcastBookRepository(hz);
        HazelcastMetadataRepository metadataStore = new HazelcastMetadataRepository(hz, new MetadataReader());
        HazelcastIndexingStatusRepository statusStore = new HazelcastIndexingStatusRepository(hz);

        JsonStopWordsProvider stopWordsLoader = new JsonStopWordsProvider();
        WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(stopWordsLoader.load());
        TermFrequencyCalculator analyzer = new TermFrequencyCalculator(tokenizer);

        IndexBookUseCase indexBook = new IndexBookUseCase(bookStore, indexStore, metadataStore, statusStore, analyzer);

        InvertedIndexRestorer recovery = new InvertedIndexRestorer(args[0], indexBook, bookStore);
        IngestionQueueCoordinator queueManager = new IngestionQueueCoordinator(hz);
        ReindexingRunnerImpl reindexingExecutor = new ReindexingRunnerImpl(recovery, hz, queueManager);

        reindexingExecutor.executeRecovery();

        ConnectionFactory jmsFactory = new ActiveMQConnectionFactory(brokerUrl);

        ReindexMessageListener rebuildListener = new ReindexMessageListener(hz, reindexingExecutor, jmsFactory);
        rebuildListener.startListening();

        EventListener messageConsumer = new ActiveMQEventListener(jmsFactory, "documents.ingested", rebuildListener);
        messageConsumer.startConsuming(documentId -> {
            log.info("Processing document from broker: {}", documentId);
            indexBook.execute(Integer.parseInt(documentId));
        });

        CoordinateReindex rebuildUseCase = new CoordinateReindex(hz, brokerUrl);

        IndexingEndpoint controller = new IndexingEndpoint(indexBook, rebuildUseCase);
        Javalin app = Javalin.create(c -> {
            c.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/document/{documentId}", controller::indexDocument);
        app.post("/index/rebuild", controller::rebuild);
        app.get("/health", controller::health);
    }
}