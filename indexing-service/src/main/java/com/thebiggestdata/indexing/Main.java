package com.thebiggestdata.indexing;

import com.thebiggestdata.indexing.infrastructure.adapters.web.IndexingController;
import com.thebiggestdata.indexing.infrastructure.adapters.recovery.CoordinateRebuild;
import com.thebiggestdata.indexing.infrastructure.adapters.tokenizer.JsonStopWordsLoader;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastBookStore;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.ActiveMQMessageConsumer;
import com.thebiggestdata.indexing.infrastructure.adapters.activemq.RebuildMessageListener;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastIndexStore;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastMetadataStore;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.MetadataParser;
import com.thebiggestdata.indexing.infrastructure.adapters.recovery.IngestionQueueManager;
import com.thebiggestdata.indexing.infrastructure.adapters.recovery.InvertedIndexRecovery;
import com.thebiggestdata.indexing.infrastructure.adapters.recovery.ReindexingExecutor;
import com.thebiggestdata.indexing.infrastructure.adapters.tokenizer.TextTokenizer;
import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.HazelcastIndexingStatusStore;
import com.thebiggestdata.indexing.application.usecases.indexingservice.IndexBook;
import com.thebiggestdata.indexing.application.usecases.indexingservice.TermFrequencyAnalyzer;
import com.thebiggestdata.indexing.infrastructure.config.HazelcastConfig;
import com.thebiggestdata.indexing.infrastructure.ports.old.MessageConsumer;
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

        MessageConsumer messageConsumer = new ActiveMQMessageConsumer(jmsFactory, "documents.ingested", rebuildListener);
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