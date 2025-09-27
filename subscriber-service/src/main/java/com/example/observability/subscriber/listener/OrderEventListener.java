package com.example.observability.subscriber.listener;

import com.example.observability.event.OrderEvent;
import com.example.observability.subscriber.service.OrderProcessingService;
import com.example.observability.util.JsonUtils;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.PubSubHeaderMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import io.micrometer.tracing.propagation.Propagator.Getter;
import java.util.Map;
import java.util.Collections;
import io.micrometer.tracing.propagation.Propagator;

/**
 * Pub/Sub message listener for processing order events asynchronously.
 * 
 * Demonstrates comprehensive observability features:
 * - Distributed tracing with trace context propagation
 * - Custom metrics for message processing
 * - Structured logging with correlation IDs
 * - Virtual threads for high-performance async processing
 * - Manual message acknowledgment for reliability
 */
@Component
@Observed(name = "order.event.listener", contextualName = "order-event-listener")
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderProcessingService orderProcessingService;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final Propagator propagator;

    // Custom metrics
    private final Counter messagesReceivedCounter;
    private final Counter messagesProcessedCounter;
    private final Counter messageProcessingFailuresCounter;
    private final Timer messageProcessingTimer;

    public OrderEventListener(OrderProcessingService orderProcessingService,
            MeterRegistry meterRegistry,
            Tracer tracer,
            Propagator propagator) {
        this.orderProcessingService = orderProcessingService;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
        this.propagator = propagator;

        // Initialize custom metrics
        this.messagesReceivedCounter = Counter.builder("pubsub.messages.received")
                .description("Number of Pub/Sub messages received")
                .tag("service", "subscriber-service")
                .tag("subscription", "orders-subscription")
                .register(meterRegistry);

        this.messagesProcessedCounter = Counter.builder("pubsub.messages.processed")
                .description("Number of Pub/Sub messages successfully processed")
                .tag("service", "subscriber-service")
                .tag("subscription", "orders-subscription")
                .register(meterRegistry);

        this.messageProcessingFailuresCounter = Counter.builder("pubsub.messages.failures")
                .description("Number of Pub/Sub message processing failures")
                .tag("service", "subscriber-service")
                .tag("subscription", "orders-subscription")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder("pubsub.message.processing.duration")
                .description("Time spent processing Pub/Sub messages")
                .tag("service", "subscriber-service")
                .tag("subscription", "orders-subscription")
                .register(meterRegistry);
    }

    /**
     * Process order event message using Spring Integration.
     * This method is automatically triggered when messages arrive on the
     * orders-subscription.
     * 
     * @param message the Pub/Sub message
     */
    @ServiceActivator(inputChannel = "ordersSubscriptionChannel")
    @Observed(name = "pubsub.message.received", contextualName = "receive-order-event", lowCardinalityKeyValues = {
            "subscription", "orders-subscription" })
    public void receiveOrderEvent(Message<String> message) {

        BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE,
                BasicAcknowledgeablePubsubMessage.class);

        Map<String, String> attributes = originalMessage != null ? originalMessage.getPubsubMessage().getAttributesMap()
                : Collections.emptyMap();

        Getter<Map<String, String>> getter = Map::get;

        // Extrai o Span.Builder do propagator
        Span.Builder extractedBuilder = propagator.extract(attributes, Map::get);

        // Cria o Span final usando start() do Builder
        Span span = extractedBuilder
                .name("pubsub-message-processing")
                .tag("subscription", "orders-subscription")
                .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            MDC.put("traceId", span.context().traceId());
            MDC.put("spanId", span.context().spanId());

            logger.info("Processing message with distributed trace context restored");

            processMessage(message.getPayload(), span);

            if (originalMessage != null) {
                originalMessage.ack();
            }
        } catch (Exception e) {
            if (originalMessage != null) {
                originalMessage.nack();
            }
            span.error(e);
        } finally {
            span.end();
            MDC.clear();
        }
    }

    /**
     * Process the message payload.
     * 
     * @param payload    the message payload
     * @param parentSpan the parent span for tracing
     */
    private void processMessage(String payload, Span parentSpan) {
        try {
            // Parse the order event from JSON
            OrderEvent orderEvent = JsonUtils.fromJson(payload, OrderEvent.class);

            if (orderEvent == null) {
                logger.error("Failed to parse order event from payload");
                messageProcessingFailuresCounter.increment();
                return;
            }

            // Add order context to span
            parentSpan.tag("order.id", orderEvent.orderId());
            parentSpan.tag("order.customer.id", orderEvent.customerId());
            parentSpan.tag("order.event.type", orderEvent.eventType().toString());

            // Set up MDC with order context
            MDC.put("orderId", orderEvent.orderId());
            MDC.put("customerId", orderEvent.customerId());
            MDC.put("eventType", orderEvent.eventType().toString());

            logger.info("Processing order event: orderId={}, eventType={}, customerId={}",
                    orderEvent.orderId(), orderEvent.eventType(), orderEvent.customerId());

            // Process the order event
            boolean processingSuccess = orderProcessingService.processOrderEvent(orderEvent);

            if (processingSuccess) {
                logger.info("Order event processed successfully: orderId={}", orderEvent.orderId());
                messagesProcessedCounter.increment();
                parentSpan.tag("processing.success", true);
            } else {
                logger.error("Failed to process order event: orderId={}", orderEvent.orderId());
                messageProcessingFailuresCounter.increment();
                parentSpan.tag("processing.success", false);
                parentSpan.tag("error", true);
            }

        } catch (Exception e) {
            logger.error("Error processing order event message: error={}", e.getMessage(), e);
            messageProcessingFailuresCounter.increment();
            parentSpan.tag("error", true);
            parentSpan.tag("error.message", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    /**
     * Get message processing metrics for health checks and monitoring.
     * 
     * @return message processing metrics summary
     */
    @Observed(name = "pubsub.listener.getMetrics", contextualName = "get-listener-metrics")
    public String getListenerMetrics() {
        return String.format(
                "MessageListenerMetrics{received=%d, processed=%d, failures=%d}",
                (long) messagesReceivedCounter.count(),
                (long) messagesProcessedCounter.count(),
                (long) messageProcessingFailuresCounter.count());
    }
}