package com.thebiggestdata.indexer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.port.DatalakeReadPort;
import com.thebiggestdata.indexer.domain.port.InvertedIndexWriterPort;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import com.thebiggestdata.indexer.domain.service.TokenNormalizer;
import com.thebiggestdata.indexer.infrastructure.adapter.HazelcastDatalakeAdapter;
import com.thebiggestdata.indexer.infrastructure.adapter.HazelcastInvertedIndexWriterAdapter;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("=== Starting Indexer Service ===");

        // 1. Conexión Hazelcast (Modo Cliente o Miembro, usaremos Cliente para Indexer)
        HazelcastInstance hazelcast = createHazelcastClient();

        // 2. Puertos y Servicios
        DatalakeReadPort reader = new HazelcastDatalakeAdapter(hazelcast);
        InvertedIndexWriterPort writer = new HazelcastInvertedIndexWriterAdapter(hazelcast, "inverted-index");
        TokenNormalizer normalizer = new TokenNormalizer();
        PostingBuilder builder = new PostingBuilder();

        // 3. Conexión ActiveMQ y Bucle de Procesamiento
        startConsumer(reader, writer, normalizer, builder);
    }

    private static void startConsumer(DatalakeReadPort reader, InvertedIndexWriterPort writer,
                                      TokenNormalizer normalizer, PostingBuilder builder) {
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");

        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("ingested.document");
            MessageConsumer consumer = session.createConsumer(destination);

            logger.info("Listening for books on {}", brokerUrl);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        // Asumiendo formato simple "BookId" o JSON. El Ingestor manda JSON, hay que parsear
                        // Para robustez rapida, intentamos extraer ID.
                        int bookId = extractIdFromMessage(text);

                        logger.info("Processing Book ID: {}", bookId);
                        processBook(bookId, reader, writer, normalizer, builder);
                    }
                } catch (Exception e) {
                    logger.error("Error processing message", e);
                }
            });

        } catch (JMSException e) {
            throw new RuntimeException("JMS Error", e);
        }
    }

    private static void processBook(int bookId, DatalakeReadPort reader, InvertedIndexWriterPort writer,
                                    TokenNormalizer normalizer, PostingBuilder builder) {
        try {
            RawDocument doc = reader.read(bookId, null);
            String normalizedText = normalizer.normalize(doc.body());
            List<String> tokens = Arrays.asList(normalizedText.split(" "));

            TokenStream stream = new TokenStream(bookId, tokens);
            Map<String, List<Integer>> postings = builder.build(stream);

            writer.write(postings);
            logger.info("Indexed Book ID: {}", bookId);
        } catch (Exception e) {
            logger.error("Failed to index Book {}", bookId, e);
        }
    }

    private static int extractIdFromMessage(String json) {
        // Extracción sucia pero eficaz para evitar dependencias de GSON en este snippet
        // {"bookId": 123, ...}
        try {
            if (json.contains("bookId")) {
                String val = json.split("bookId\":")[1].split(",")[0].trim();
                return Integer.parseInt(val.replaceAll("[^0-9]", ""));
            }
            return Integer.parseInt(json); // Fallback si es solo numero
        } catch (Exception e) {
            return -1;
        }
    }

    private static HazelcastInstance createHazelcastClient() {
        ClientConfig config = new ClientConfig();
        config.setClusterName(System.getenv().getOrDefault("HZ_CLUSTER_NAME", "SearchEngine"));
        String members = System.getenv().getOrDefault("HZ_MEMBERS", "localhost:5701");
        for(String m : members.split(",")) config.getNetworkConfig().addAddress(m);
        return HazelcastClient.newHazelcastClient(config);
    }
}
