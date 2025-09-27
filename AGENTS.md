# AGENTS.md - Observability Reference Application

## Project Overview

This is a comprehensive Java 21 observability reference application demonstrating enterprise-grade monitoring, tracing, and logging patterns. The application implements a distributed order processing system with full observability across all three pillars: **metrics, traces, and logs**.

### Architecture
- **API Service** (port 8080): REST API for order management
- **Subscriber Service** (port 8081): Async message processor via GCP Pub/Sub
- **Infrastructure**: PostgreSQL, Pub/Sub emulator, OpenTelemetry, Prometheus, Jaeger, Grafana, Loki
- **Framework**: Spring Boot 3.2.4 with Java 21 virtual threads
- **Observability**: OpenTelemetry BOM 1.35.0 + Micrometer 1.12.4

### Key Features
- **Distributed Tracing**: End-to-end trace propagation through HTTP → Database → Pub/Sub → Subscriber
- **Custom Metrics**: Business and technical metrics via Micrometer
- **Structured Logging**: JSON logs with trace correlation
- **Virtual Threads**: Java 21 preview features enabled

## Build and Test Commands

### Prerequisites
```bash
# Required tools
java --version  # Must be Java 21+
mvn --version   # Must be Maven 3.9+
docker --version && docker-compose --version
```

### Infrastructure Setup
```bash
# Start all infrastructure services (required first)
docker-compose up -d postgres pubsub-emulator otel-collector jaeger prometheus grafana loki promtail

# Verify infrastructure health
docker-compose ps
# All services should show "Up" status
```

### Build Commands
```bash
# Clean build (removes all artifacts)
mvn clean

# Build without tests (faster for development)
mvn clean package -DskipTests

# Full build with tests
mvn clean package

# Build specific module
cd api-service && mvn package
cd subscriber-service && mvn package
```

### Test Commands
```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl api-service
mvn test -pl subscriber-service

# Run integration tests (requires infrastructure)
mvn verify -Dspring.profiles.active=test
```

### Application Startup
```bash
# API Service (Terminal 1)
cd api-service
OTEL_SERVICE_NAME=api-service \
OTEL_RESOURCE_ATTRIBUTES=service.name=api-service,service.version=1.0.0,deployment.environment=development \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
OTEL_METRICS_EXPORTER=none \
OTEL_LOGS_EXPORTER=none \
java --enable-preview \
-javaagent:../opentelemetry-javaagent.jar \
-jar target/api-service-1.0.0-SNAPSHOT.jar

# Subscriber Service (Terminal 2)
cd subscriber-service
OTEL_SERVICE_NAME=subscriber-service \
OTEL_RESOURCE_ATTRIBUTES=service.name=subscriber-service,service.version=1.0.0,deployment.environment=development \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
OTEL_METRICS_EXPORTER=none \
OTEL_LOGS_EXPORTER=none \
java --enable-preview \
-javaagent:../opentelemetry-javaagent.jar \
-jar target/subscriber-service-1.0.0-SNAPSHOT.jar
```

### Health Check Commands
```bash
# Infrastructure health
curl http://localhost:8080/actuator/health  # API service
curl http://localhost:8081/actuator/health  # Subscriber service

# Metrics endpoints
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus

# Observability UIs
open http://localhost:3001  # Grafana (admin/admin)
open http://localhost:9090  # Prometheus
open http://localhost:16686 # Jaeger
```

## Code Style Guidelines

### Java Conventions
- **Java 21 Features**: Use virtual threads, pattern matching, records where appropriate
- **Naming**: PascalCase for classes, camelCase for methods/variables, UPPER_SNAKE_CASE for constants
- **Packages**: `com.example.observability.{module}.{layer}` structure
- **Annotations**: Spring annotations preferred over XML configuration

### Observability Code Style
```java
// ✅ Good: Comprehensive observability
@Component
@Observed(name = "order.processing", contextualName = "process-order")
public class OrderService {

    private final Counter orderCounter = Counter.builder("orders.created")
        .description("Number of orders created")
        .tag("service", "api-service")
        .register(meterRegistry);

    @Timed(name = "order.creation.duration", description = "Time to create order")
    public OrderResponse createOrder(OrderRequest request) {
        Span span = tracer.nextSpan()
            .name("order-creation")
            .tag("customer.id", request.customerId())
            .tag("order.amount", request.amount().toString())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            orderCounter.increment();
            // Business logic here
            return response;
        } catch (Exception e) {
            span.tag("error", true);
            span.tag("error.message", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Database Layer Style
```java
// ✅ Good: JPA with observability
@Entity
@Table(name = "orders")
public class OrderEntity {
    // Use records for value objects
    // Use @Observed for repository methods
    // Add proper validation annotations
}

@Repository
@Observed(name = "order.repository")
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Custom queries with @Query annotation
    // Use proper naming conventions
}
```

### Configuration Style
```java
// ✅ Good: Configuration with validation
@Configuration
@EnableConfigurationProperties
@Validated
public class ObservabilityConfig {

    @Bean
    @ConditionalOnProperty(name = "observability.metrics.enabled", havingValue = "true")
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
```

## Testing Instructions

### Test Structure
```
src/test/java/
├── unit/           # Unit tests (fast, no external dependencies)
├── integration/    # Integration tests (database, messaging)
└── e2e/           # End-to-end tests (full application)
```

### Unit Tests
```java
// ✅ Good: Unit test with observability verification
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter orderCounter;

    @Test
    void shouldIncrementCounterWhenOrderCreated() {
        // Given
        when(meterRegistry.counter(any())).thenReturn(orderCounter);

        // When
        orderService.createOrder(validRequest);

        // Then
        verify(orderCounter).increment();
    }
}
```

### Integration Tests
```java
// ✅ Good: Integration test with test containers
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.gcp.pubsub.emulator-host=localhost:8085",
    "observability.tracing.enabled=false"
})
class OrderIntegrationTest {

    @Test
    void shouldProcessOrderEndToEnd() {
        // Test complete flow: API → Database → Pub/Sub → Subscriber
    }
}
```

### Test Data Management
```java
// ✅ Good: Test data builders
public class OrderTestDataBuilder {
    public static OrderRequest validOrderRequest() {
        return new OrderRequest(
            "test-customer-id",
            "Test Product",
            1,
            BigDecimal.valueOf(99.99)
        );
    }
}
```

### Running Tests with Observability
```bash
# Tests with metrics collection
mvn test -Dspring.profiles.active=test -Dobservability.metrics.enabled=true

# Tests with tracing (requires collector)
mvn test -Dspring.profiles.active=test -Dobservability.tracing.enabled=true
```

## Security Considerations

### Secrets Management
- **NO HARDCODED SECRETS**: All credentials via environment variables or external config
- **Database**: Use connection pooling with proper authentication
- **Pub/Sub**: Emulator for local, proper IAM for production
- **Observability**: No sensitive data in metrics, traces, or logs

### Application Security
```java
// ✅ Good: Secure configuration
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").hasRole("MONITOR")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### Data Privacy in Observability
```java
// ✅ Good: Sanitized observability data
public class OrderObservabilityHelper {

    public static Span addOrderSpanTags(Span span, OrderRequest request) {
        return span
            .tag("customer.id", hashCustomerId(request.customerId())) // Hash PII
            .tag("order.amount.range", getAmountRange(request.amount())) // Bucket amounts
            .tag("product.category", getProductCategory(request.productName())); // Category not name
    }

    private static String hashCustomerId(String customerId) {
        // Use consistent hashing to maintain correlation while protecting PII
        return DigestUtils.sha256Hex(customerId).substring(0, 8);
    }
}
```

### Infrastructure Security
- **Network**: All services communicate via Docker internal networks
- **Ports**: Only necessary ports exposed to host
- **Volumes**: Proper volume permissions and data encryption at rest
- **Images**: Use official images with security updates

## Project-Specific Guidelines

### Observability Standards
1. **Every public method** should have observability (metrics, tracing, or logging)
2. **Custom metrics** must include service tags and proper descriptions
3. **Traces** must include business context (customer ID, order ID, etc.)
4. **Logs** must be structured JSON with correlation IDs
5. **Errors** must be properly recorded in spans with context

### Database Guidelines
- **Schema Evolution**: Use Flyway migrations, never alter production directly
- **Connection Pooling**: Always use HikariCP with proper metrics
- **Transactions**: Use @Transactional with proper propagation
- **Query Performance**: Use @Query with pagination for large datasets

### Messaging Guidelines
- **Idempotency**: All message handlers must be idempotent
- **Error Handling**: Use dead letter queues for poison messages
- **Trace Propagation**: Always propagate trace context through message attributes
- **Acknowledgment**: Use manual acknowledgment for reliable processing

### Virtual Threads Usage
```java
// ✅ Good: Virtual threads for I/O bound operations
@Service
public class OrderProcessingService {

    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> processOrderAsync(OrderEvent event) {
        // I/O bound work benefits from virtual threads
        return CompletableFuture.completedFuture(null);
    }
}

@Configuration
public class VirtualThreadConfig {

    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### Configuration Management
- **Profiles**: Use Spring profiles for environment-specific config
- **Properties**: Group related properties with common prefixes
- **Validation**: Use @ConfigurationProperties with validation
- **Documentation**: Document all configuration properties

### Performance Guidelines
- **Metrics**: Use timers for all I/O operations
- **Caching**: Cache database queries where appropriate with metrics
- **Connection Pools**: Monitor and tune connection pool sizes
- **Batch Processing**: Use batch operations for bulk database operations

## Troubleshooting Common Issues

### Build Issues
```bash
# Java version issues
update-alternatives --config java
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Dependency conflicts
mvn dependency:tree
mvn clean install -U
```

### Runtime Issues
```bash
# Port conflicts
netstat -tulpn | grep :8080
lsof -i :8080

# Memory issues
java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx2g -jar app.jar

# OpenTelemetry agent issues
java -javaagent:opentelemetry-javaagent.jar -Dotel.javaagent.debug=true -jar app.jar
```

### Observability Issues
```bash
# Check collector health
curl http://localhost:13133/

# Check metrics export
curl http://localhost:8080/actuator/prometheus | grep order

# Check Jaeger spans
curl "http://localhost:16686/api/traces?service=api-service&limit=10"
```

## Development Workflow

### Feature Development
1. **Create feature branch** from main
2. **Write tests first** (TDD approach)
3. **Implement with observability** (metrics, tracing, logging)
4. **Validate locally** with full infrastructure
5. **Create pull request** with observability evidence

### Commit Message Format
```
type(scope): brief description

- Detailed explanation of changes
- Include observability impact
- Reference any metrics/traces added

Fixes: #issue-number
```

Example:
```
feat(api): add order creation endpoint

- Implement POST /api/orders with validation
- Add custom metrics: orders.created counter
- Add distributed tracing with customer context
- Include structured logging with correlation IDs

Fixes: #123
```

### Pull Request Guidelines
- **Include observability screenshots** (Grafana/Jaeger/Prometheus)
- **Document new metrics/traces** in PR description
- **Verify all health checks pass**
- **Include load test results** for performance changes

This application serves as a comprehensive reference for building observable microservices with Java 21, Spring Boot, and modern observability tools.