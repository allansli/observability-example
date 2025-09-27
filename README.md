# Observability Reference Application

A comprehensive observability reference application built with Java 21, Spring Boot, OpenTelemetry, and Micrometer, demonstrating observability-first architecture with the three pillars: metrics, traces, and logs.

## 🏗️ Architecture Overview

This application demonstrates a complete observability stack for a distributed order processing system:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   REST API      │───▶│   PostgreSQL     │    │  GCP Pub/Sub    │
│   (Port 8080)   │    │   Database       │    │   Emulator      │
└─────────┬───────┘    └──────────────────┘    └─────────┬───────┘
          │                                              │
          ▼                                              ▼
┌─────────────────┐                             ┌─────────────────┐
│  Pub/Sub Topic  │───────────────────────────▶│   Subscriber    │
│   (orders)      │                            │   Service       │
└─────────────────┘                            │  (Port 8081)    │
                                               └─────────┬───────┘
                                                         │
                                                         ▼
                                               ┌─────────────────┐
                                               │   PostgreSQL    │
                                               │   (Update)      │
                                               └─────────────────┘
```

### Application Flow

1. **REST API** receives POST request with order data
2. **Database Query** saves order to PostgreSQL with PENDING status
3. **Message Publishing** publishes order event to GCP Pub/Sub
4. **Async Processing** subscriber consumes message from Pub/Sub
5. **Database Update** subscriber updates order status to PROCESSED

### Observability Stack

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Applications  │───▶│ OpenTelemetry    │───▶│     Jaeger      │
│  (Traces/Logs)  │    │   Collector      │    │   (Traces)      │
└─────────────────┘    └─────────┬────────┘    └─────────────────┘
                                 │
                                 ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Prometheus    │◀───│   Micrometer     │    │    Grafana      │
│   (Metrics)     │    │   Registry       │───▶│ (Visualization) │
└─────────────────┘    └──────────────────┘    └─────────┬───────┘
                                                         │
┌─────────────────┐    ┌──────────────────┐              │
│      Loki       │◀───│    Promtail      │◀─────────────┘
│     (Logs)      │    │ (Log Collection) │
└─────────────────┘    └──────────────────┘
```

## 🚀 Technology Stack

### Core Technologies
- **Java 21** with Virtual Threads for high-performance concurrent processing
- **Spring Boot 3.2.4** with comprehensive observability auto-configuration
- **Maven** multi-module project structure for clean separation

### Observability Stack
- **OpenTelemetry BOM 1.35.0** for distributed tracing and metrics
- **Micrometer 1.12.4** for application metrics and monitoring
- **Micrometer Tracing** bridge for OpenTelemetry integration
- **Structured Logging** with Logback and JSON formatting

### Infrastructure
- **PostgreSQL 15** with connection pool observability
- **GCP Pub/Sub** (emulator) for asynchronous messaging
- **Docker Compose** for complete infrastructure orchestration

### Monitoring & Visualization
- **Prometheus** for metrics collection and storage
- **Grafana** with pre-configured dashboards
- **Jaeger** for distributed trace visualization
- **Loki + Promtail** for centralized log aggregation

## 🔧 Docker Compose Services

| Service | Port | Purpose | Health Check |
|---------|------|---------|--------------|
| **postgres** | 5432 | PostgreSQL database | `pg_isready` |
| **pubsub-emulator** | 8085 | GCP Pub/Sub emulator | HTTP health |
| **otel-collector** | 4317/4318 | OpenTelemetry data collection | HTTP health |
| **jaeger** | 16686 | Distributed tracing UI | Built-in |
| **prometheus** | 9090 | Metrics collection | Built-in |
| **grafana** | 3001 | Visualization dashboard | Built-in |
| **loki** | 3100 | Log aggregation | Built-in |
| **promtail** | - | Log collection | Built-in |
| **api-service** | 8080 | REST API service | Custom endpoint |
| **subscriber-service** | 8081 | Message processing | Actuator health |

## 📊 OpenTelemetry Configuration

### Trace Collection
- **OTLP Receivers**: gRPC (4317) and HTTP (4318)
- **Batch Processing**: Optimized for performance with 1024 batch size
- **Resource Attribution**: Consistent service metadata
- **Sampling**: 100% for demo (configurable for production)

### Metrics Export
- **Prometheus Integration**: Native metrics export
- **Custom Metrics**: Business-specific counters and timers
- **JVM Metrics**: Memory, GC, thread pool monitoring
- **Database Metrics**: Connection pool and query performance

### Trace Export
- **Jaeger Integration**: Direct OTLP export to Jaeger
- **Correlation**: Automatic trace-log correlation via trace/span IDs

## 🎯 Micrometer Integration Strategy

### Custom Metrics
```java
// Business Metrics
Counter orderCreatedCounter = Counter.builder("orders.created")
    .description("Number of orders created")
    .tag("service", "api-service")
    .register(meterRegistry);

Timer databaseTimer = Timer.builder("database.operations")
    .description("Time spent on database operations")
    .register(meterRegistry);

// Infrastructure Metrics
Timer pubsubTimer = Timer.builder("pubsub.operations")
    .description("Time spent on Pub/Sub operations")
    .register(meterRegistry);
```

### Observability Annotations
```java
@Observed(name = "order.service.createOrder",
          contextualName = "create-order",
          lowCardinalityKeyValues = {"operation", "create"})
public OrderResponse createOrder(OrderRequest request) {
    // Implementation with automatic metrics
}
```

### Connection Pool Monitoring
- **HikariCP Integration**: Built-in connection pool metrics
- **Database Performance**: Query timing and connection usage
- **Resource Utilization**: Pool size optimization insights

## 📈 Observability Data Flow

### Metrics Flow
```
Application (Micrometer) 
    ↓
Actuator Prometheus Endpoint (/actuator/prometheus)
    ↓
OpenTelemetry Collector (Prometheus Receiver)
    ↓
Prometheus (Metrics Storage)
    ↓
Grafana (Visualization)
```

### Traces Flow
```
Application (OpenTelemetry API)
    ↓ 
OpenTelemetry SDK (Auto-instrumentation)
    ↓
OTLP HTTP Exporter (port 4318)
    ↓
OpenTelemetry Collector (OTLP Receiver)
    ↓
Jaeger (Trace Storage & UI)
```

### Logs Flow
```
Application (Logback JSON)
    ↓
File System (Structured JSON logs)
    ↓
Promtail (Log Collection)
    ↓
Loki (Log Storage)
    ↓
Grafana (Log Visualization)
```

## 🔄 Implementation Plan with Validation Points

### Phase 1: Project Structure & Dependencies
- [x] **Maven multi-module setup** with parent POM
- [x] **Shared models** for data consistency
- [x] **Dependency management** with BOMs
- **Validation**: `mvn clean compile` succeeds

### Phase 2: API Service Implementation
- [x] **REST Controller** with comprehensive observability
- [x] **Service Layer** with custom metrics and tracing
- [x] **Database Layer** with JPA and connection pool monitoring
- [x] **Pub/Sub Publisher** with distributed tracing
- **Validation**: Service starts and health check passes

### Phase 3: Subscriber Service Implementation
- [x] **Message Consumer** with trace context propagation
- [x] **Processing Service** with business metrics
- [x] **Database Updates** with observability
- [x] **Error Handling** with proper trace recording
- **Validation**: Messages processed successfully

### Phase 4: Observability Infrastructure
- [x] **OpenTelemetry Collector** configuration
- [x] **Prometheus** metrics collection setup
- [x] **Jaeger** distributed tracing
- [x] **Grafana** dashboards and datasources
- [x] **Loki + Promtail** log aggregation
- **Validation**: All observability endpoints accessible

### Phase 5: Docker Infrastructure
- [x] **Multi-service Docker Compose**
- [x] **Service Dependencies** and health checks
- [x] **Volume Management** for data persistence
- [x] **Network Configuration** for service communication
- **Validation**: `docker-compose up` starts all services

### Phase 6: Testing & Validation
- [x] **Automated Test Scripts** for API validation
- [x] **Load Generation** for observability data
- [x] **Health Checks** for all services
- [x] **Integration Testing** end-to-end flow
- **Validation**: Complete trace visible from API to processing

## 🎯 Key Observability Features

### Distributed Tracing
- **End-to-end visibility** from HTTP request to database update
- **Trace correlation** across service boundaries via Pub/Sub
- **Error tracking** with exception details and stack traces
- **Performance insights** with span timing and attributes

### Comprehensive Metrics
- **Business metrics**: Order creation/processing rates
- **Technical metrics**: HTTP request duration, database timing
- **Infrastructure metrics**: JVM memory, GC, connection pools
- **Error metrics**: Failure rates and error classifications

### Structured Logging
- **JSON format** for machine-readable logs
- **Correlation IDs** for request tracking
- **Contextual metadata** (customer ID, order ID, trace ID)
- **Log-trace correlation** via OpenTelemetry integration

### Real-time Dashboards
- **Service health overview** with key performance indicators
- **Database performance** with connection pool metrics
- **Message processing** with Pub/Sub throughput
- **Error analysis** with failure rate trending

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21 JDK (for development)
- Maven 3.9+ (for development)
- 8GB+ RAM for all services

### Quick Start
```bash
# Clone and start all services
git clone [repository-url]
cd observability
./scripts/start.sh

# Generate test data
./scripts/test-api.sh
```

### Access Points
- **API Service**: http://localhost:8080
- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Jaeger**: http://localhost:16686

### Validation Checklist
- [ ] All services healthy: `docker-compose ps`
- [ ] API responds: `curl http://localhost:8080/api/v1/orders/health`
- [ ] Metrics available: `curl http://localhost:8080/actuator/prometheus`
- [ ] Traces in Jaeger: Check UI after API calls
- [ ] Grafana dashboards: Verify data visualization

## 🔍 Observability Best Practices Demonstrated

### 1. Observability-First Design
- **Instrumentation as code**: Metrics and tracing built into business logic
- **Semantic conventions**: Consistent attribute naming and structure
- **Error handling**: Proper exception recording and error attribution

### 2. Performance Optimization
- **Sampling strategies**: Configurable trace sampling rates
- **Batch processing**: Efficient data export with batching
- **Resource management**: Proper cleanup and resource limits

### 3. Operational Excellence
- **Health checks**: Comprehensive service health monitoring
- **Graceful degradation**: Continue operation during observability failures
- **Cost awareness**: Balanced observability coverage vs. overhead

### 4. Developer Experience
- **Local development**: Complete stack runnable locally
- **Testing integration**: Automated tests with observability validation
- **Documentation**: Comprehensive setup and troubleshooting guides

This reference application provides a production-ready foundation for building observable microservices with comprehensive monitoring, tracing, and logging capabilities.