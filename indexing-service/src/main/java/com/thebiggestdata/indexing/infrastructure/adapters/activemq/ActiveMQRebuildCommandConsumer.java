package com.thebiggestdata.indexing.infrastructure.adapters.activemq;

import com.thebiggestdata.indexing.infrastructure.ports.RebuildCommandConsumer;
import com.thebiggestdata.indexing.model.RebuildCommand;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ActiveMQRebuildCommandConsumer implements RebuildCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActiveMQRebuildCommandConsumer.class);

    private final ConnectionFactory connectionFactory;
    private final RebuildCommandMessageMapper mapper;
    private final String consumerId;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Connection activeConnection;

    public ActiveMQRebuildCommandConsumer(
            ConnectionFactory connectionFactory,
            RebuildCommandMessageMapper mapper,
            String nodeId
    ) {
        this.connectionFactory = connectionFactory;
        this.mapper = mapper;
        this.consumerId = "index-rebuild-" + nodeId.replace(':', '-');
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "index-rebuild-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void start(Consumer<RebuildCommand> handler) {
        if (running.compareAndSet(false, true)) {
            executor.submit(() -> consumeWithReconnect(handler));
        }
    }

    private void consumeWithReconnect(Consumer<RebuildCommand> handler) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                consume(handler);
            } catch (JMSException e) {
                if (running.get()) {
                    log.warn("Rebuild command consumer disconnected; retrying", e);
                    sleepBeforeReconnect();
                }
            }
        }
    }

    private void consume(Consumer<RebuildCommand> handler) throws JMSException {
        try (Connection connection = connectionFactory.createConnection()) {
            activeConnection = connection;
            connection.setClientID(consumerId);
            connection.start();
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Topic topic = session.createTopic(ActiveMQRebuildCommandPublisher.TOPIC);
                try (MessageConsumer consumer = session.createDurableConsumer(topic, consumerId)) {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        Message message = consumer.receive(1_000);
                        if (message != null) {
                            handle(message, session, handler);
                        }
                    }
                }
            } finally {
                activeConnection = null;
            }
        }
    }

    private void handle(Message message, Session session, Consumer<RebuildCommand> handler) throws JMSException {
        try {
            if (!(message instanceof TextMessage textMessage)) {
                throw new IllegalArgumentException("Only text rebuild commands are supported");
            }
            handler.accept(mapper.fromJson(textMessage.getText()));
            session.commit();
        } catch (RuntimeException e) {
            log.error("Rebuild command failed and will be redelivered", e);
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
        Connection connection = activeConnection;
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ignored) {
            }
        }
        executor.shutdownNow();
    }
}
