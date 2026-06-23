package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.thebiggestdata.indexing.infrastructure.ports.IndexingEventConsumer;
import com.thebiggestdata.indexing.model.BookIngestedEvent;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ActiveMQIndexingEventConsumer implements IndexingEventConsumer {

    public static final String QUEUE = "documents.ingested";

    private static final Logger log = LoggerFactory.getLogger(ActiveMQIndexingEventConsumer.class);

    private final ConnectionFactory connectionFactory;
    private final BookIngestedMessageMapper mapper;
    private final int concurrentConsumers;
    private final ExecutorService workers;
    private final Set<Connection> activeConnections = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean();

    public ActiveMQIndexingEventConsumer(
            ConnectionFactory connectionFactory,
            BookIngestedMessageMapper mapper,
            int concurrentConsumers
    ) {
        if (concurrentConsumers < 1) {
            throw new IllegalArgumentException("concurrentConsumers must be positive");
        }
        this.connectionFactory = connectionFactory;
        this.mapper = mapper;
        this.concurrentConsumers = concurrentConsumers;
        this.workers = Executors.newFixedThreadPool(concurrentConsumers, runnable -> {
            Thread thread = new Thread(runnable, "indexing-event-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void start(Consumer<BookIngestedEvent> handler) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        for (int worker = 0; worker < concurrentConsumers; worker++) {
            workers.submit(() -> consumeWithReconnect(handler));
        }
    }

    private void consumeWithReconnect(Consumer<BookIngestedEvent> handler) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                consume(handler);
            } catch (JMSException e) {
                if (running.get()) {
                    log.warn("Indexing queue consumer disconnected; retrying", e);
                    sleepBeforeReconnect();
                }
            }
        }
    }

    private void consume(Consumer<BookIngestedEvent> handler) throws JMSException {
        try (Connection connection = connectionFactory.createConnection()) {
            activeConnections.add(connection);
            connection.start();
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Queue queue = session.createQueue(QUEUE);
                try (MessageConsumer consumer = session.createConsumer(queue)) {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        Message message = consumer.receive(1_000);
                        if (message != null) {
                            handle(message, session, handler);
                        }
                    }
                }
            } finally {
                activeConnections.remove(connection);
            }
        }
    }

    private void handle(
            Message message,
            Session session,
            Consumer<BookIngestedEvent> handler
    ) throws JMSException {
        try {
            if (!(message instanceof TextMessage textMessage)) {
                throw new IllegalArgumentException("Only text indexing events are supported");
            }
            handler.accept(mapper.fromJson(textMessage.getText()));
            session.commit();
        } catch (RuntimeException e) {
            log.error("Indexing event failed and will be redelivered", e);
            session.rollback();
        }
    }

    private void sleepBeforeReconnect() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running.set(false);
        activeConnections.forEach(this::closeQuietly);
        workers.shutdownNow();
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (JMSException ignored) {
        }
    }
}
