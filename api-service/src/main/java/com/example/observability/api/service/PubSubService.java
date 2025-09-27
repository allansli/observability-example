package com.example.observability.api.service;

import com.example.observability.event.OrderEvent;
import com.example.observability.util.JsonUtils;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PubSubService {

    private static final Logger logger = LoggerFactory.getLogger(PubSubService.class);

    private final PubSubTemplate pubSubTemplate;
    private final String topicName;
    private final Tracer tracer;
    private final Propagator propagator;

    // Metrics
    private final Counter messagesPublishedCounter;
    private final Counter messagePublishFailuresCounter;

    public PubSubService(PubSubTemplate pubSubTemplate,
            @Value("${app.pubsub.topic-name}") String topicName,
            Tracer tracer,
            MeterRegistry meterRegistry,
            Propagator propagator) {
        this.pubSubTemplate = pubSubTemplate;
        this.topicName = topicName;
        this.tracer = tracer;
        this.propagator = propagator;

        // Initialize metrics
        this.messagesPublishedCounter = Counter.builder("pubsub.messages.published")
                .description("Number of messages published to Pub/Sub")
                .tag("service", "api-service")
                .tag("topic", topicName)
                .register(meterRegistry);

        this.messagePublishFailuresCounter = Counter.builder("pubsub.messages.publish.failures")
                .description("Number of failed message publications")
                .tag("service", "api-service")
                .tag("topic", topicName)
                .register(meterRegistry);
    }

    @Timed(value = "pubsub.publish", description = "Time taken to publish a message")
    public CompletableFuture<Void> publishOrderEvent(OrderEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            // Cria o span de publicação
            Span span = tracer.nextSpan()
                    .name("pubsub-publish")
                    .tag("messaging.system", "gcp_pubsub")
                    .tag("messaging.destination", topicName)
                    .tag("messaging.operation", "publish")
                    .tag("event.type", event.eventType().toString())
                    .tag("order.id", event.orderId())
                    .start();

            try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
                var traceContext = span.context();
                var eventWithTrace = event.withTraceContext(
                        traceContext.traceId(),
                        traceContext.spanId());

                // Serializa o payload
                var messagePayload = JsonUtils.toJson(eventWithTrace);

                // Cria atributos básicos do evento
                Map<String, String> attributes = new HashMap<>();
                attributes.put("eventType", eventWithTrace.eventType().toString());
                attributes.put("orderId", eventWithTrace.orderId());
                attributes.put("customerId", eventWithTrace.customerId());
                attributes.put("source", eventWithTrace.source());

                // 🔑 Injeta o contexto de trace nos atributos
                propagator.inject(span.context(), attributes, Map::put);

                // Publica no Pub/Sub
                var messageId = pubSubTemplate.publish(topicName, messagePayload, attributes);

                logger.info("Published {} event for order {} with message ID: {} traceId={} spanId={}",
                        eventWithTrace.eventType(),
                        eventWithTrace.orderId(),
                        messageId.get(),
                        traceContext.traceId(),
                        traceContext.spanId());

                messagesPublishedCounter.increment();
                span.tag("messaging.message_id", messageId.get().toString());
                span.event("message.published");

                return null;
            } catch (Exception e) {
                messagePublishFailuresCounter.increment();
                span.tag("error", true);
                span.tag("error.message", e.getMessage());

                logger.error("Failed to publish {} event for order {}",
                        event.eventType(), event.orderId(), e);
                throw new RuntimeException("Failed to publish message to Pub/Sub", e);
            } finally {
                span.end();
            }
        });
    }

    @Timed(value = "pubsub.publish.sync", description = "Time taken to publish a message synchronously")
    public void publishOrderEventSync(OrderEvent event) {
        publishOrderEvent(event).join();
    }
}