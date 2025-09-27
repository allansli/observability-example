package com.example.observability.subscriber.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * Spring Integration configuration for Google Cloud Pub/Sub.
 * Sets up the message channels and adapters for automatic message processing.
 */
@Configuration
public class PubSubIntegrationConfig {

    private static final Logger logger = LoggerFactory.getLogger(PubSubIntegrationConfig.class);

    /**
     * Creates a message channel for the orders subscription.
     * Messages from the subscription will be delivered to this channel.
     */
    @Bean
    public MessageChannel ordersSubscriptionChannel() {
        logger.info("Creating orders-subscription-channel for Pub/Sub integration");
        return new DirectChannel();
    }

    /**
     * Creates an inbound channel adapter that pulls messages from the Pub/Sub subscription
     * and delivers them to the orders-subscription-channel.
     */
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("ordersSubscriptionChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        
        logger.info("Creating PubSubInboundChannelAdapter for orders-subscription");
        
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
                pubSubTemplate, "orders-subscription");
        
        adapter.setOutputChannel(inputChannel);
        adapter.setPayloadType(String.class);
        
        logger.info("PubSubInboundChannelAdapter configured successfully");
        
        return adapter;
    }
}