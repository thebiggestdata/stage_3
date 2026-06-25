package com.thebiggestdata.indexing.infrastructure.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;

public final class ActiveMQConfig {

    public ActiveMQConnectionFactory create(
            String brokerUrl,
            int queuePrefetch,
            int maxRedeliveries
    ) {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        ActiveMQPrefetchPolicy prefetch = new ActiveMQPrefetchPolicy();
        prefetch.setQueuePrefetch(queuePrefetch);
        factory.setPrefetchPolicy(prefetch);
        factory.setDispatchAsync(true);

        RedeliveryPolicy redelivery = new RedeliveryPolicy();
        redelivery.setMaximumRedeliveries(maxRedeliveries);
        redelivery.setInitialRedeliveryDelay(500);
        redelivery.setUseExponentialBackOff(true);
        redelivery.setBackOffMultiplier(2);
        factory.setRedeliveryPolicy(redelivery);
        return factory;
    }
}
