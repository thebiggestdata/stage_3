package com.thebiggestdata;

import com.thebiggestdata.infrastructure.adapter.web.IndexingController;
import com.thebiggestdata.infrastructure.adapter.recovery.CoordinateRebuild;
import com.thebiggestdata.infrastructure.adapter.tokenizer.JsonStopWordsLoader;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastBookStore;
import com.thebiggestdata.infrastructure.adapter.activemq.ActiveMQMessageConsumer;
import com.thebiggestdata.infrastructure.adapter.activemq.RebuildMessageListener;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIndexStore;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastMetadataStore;
import com.thebiggestdata.infrastructure.adapter.hazelcast.MetadataParser;
import com.thebiggestdata.infrastructure.adapter.recovery.IngestionQueueManager;
import com.thebiggestdata.infrastructure.adapter.recovery.InvertedIndexRecovery;
import com.thebiggestdata.infrastructure.adapter.recovery.ReindexingExecutor;
import com.thebiggestdata.infrastructure.adapter.tokenizer.TextTokenizer;
import com.thebiggestdata.infrastructure.adapter.hazelcast.HazelcastIndexingStatusStore;
import com.thebiggestdata.usecase.IndexBook;
import com.thebiggestdata.usecase.TermFrequencyAnalyzer;
import com.thebiggestdata.infrastructure.config.HazelcastConfig;
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

        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hz = hazelcastConfig.initHazelcast(clusterName);

        HazelcastIndexStore indexStore = new HazelcastIndexStore(hz);
        HazelcastBookStore bookStore = new HazelcastBookStore(hz);
        HazelcastMetadataStore metadataStore = new HazelcastMetadataStore(hz, new MetadataParser());
        HazelcastIndexingStatusStore statusStore = new HazelcastIndexingStatusStore(hz);

        JsonStopWordsLoader stopWordsLoader = new JsonStopWordsLoader();
        TextTokenizer tokenizer = new TextTokenizer(stopWordsLoader.load());
        TermFrequencyAnalyzer analyzer = new TermFrequencyAnalyzer(tokenizer);

        IndexBook indexBook = new IndexBook(bookStore, indexStore, metadataStore, statusStore, analyzer);

        InvertedIndexRecovery recovery = new InvertedIndexRecovery(args[0], indexBook, bookStore);
        IngestionQueueManager queueManager = new IngestionQueueManager(hz);
        ReindexingExecutor reindexingExecutor = new ReindexingExecutor(recovery, hz, queueManager);

        reindexingExecutor.executeRecovery();

        ConnectionFactory jmsFactory = new ActiveMQConnectionFactory(brokerUrl);

        RebuildMessageListener rebuildListener = new RebuildMessageListener(hz, reindexingExecutor, jmsFactory);
        rebuildListener.startListening();

        EventListener messageConsumer = new ActiveMQMessageConsumer(jmsFactory, "documents.ingested", rebuildListener);
        messageConsumer.startConsuming(documentId -> {
            log.info("Processing document from broker: {}", documentId);
            indexBook.execute(Integer.parseInt(documentId));
        });

        CoordinateRebuild rebuildUseCase = new CoordinateRebuild(hz, brokerUrl);

        IndexingController controller = new IndexingController(indexBook, rebuildUseCase);
        Javalin app = Javalin.create(c -> {
            c.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/document/{documentId}", controller::indexDocument);
        app.post("/index/rebuild", controller::rebuild);
        app.get("/health", controller::health);
    }
}