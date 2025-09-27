package com.example.observability.subscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Cloud Pub/Sub integration.
 * The actual message listener is implemented using @PubsubListener annotation
 * in the OrderEventListener class.
 */
@Configuration
public class PubSubConfig {

    private static final Logger logger = LoggerFactory.getLogger(PubSubConfig.class);
    
    public PubSubConfig() {
        logger.info("Pub/Sub configuration initialized - using @PubsubListener for message consumption");
    }
}