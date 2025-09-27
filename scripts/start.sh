#!/bin/bash

set -e

echo "🚀 Starting Observability Reference Application..."

# Check if Docker and Docker Compose are available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create log directories
echo "📁 Creating log directories..."
mkdir -p api-service/logs
mkdir -p subscriber-service/logs

# Set permissions
chmod 777 api-service/logs
chmod 777 subscriber-service/logs

# Start infrastructure services first
echo "🏗️ Starting infrastructure services..."
docker-compose up -d postgres pubsub-emulator otel-collector jaeger prometheus grafana loki promtail

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
until docker-compose exec postgres pg_isready -U orders_user -d orders_db -q; do
    echo "PostgreSQL is not ready yet, waiting..."
    sleep 2
done
echo "✅ PostgreSQL is ready!"

# Wait for Pub/Sub emulator to be ready
echo "⏳ Waiting for Pub/Sub emulator to be ready..."
until curl -f http://localhost:8085 &>/dev/null; do
    echo "Pub/Sub emulator is not ready yet, waiting..."
    sleep 2
done
echo "✅ Pub/Sub emulator is ready!"

# Build and start application services
echo "🔨 Building and starting application services..."
docker-compose up -d --build api-service subscriber-service

echo ""
echo "🎉 Observability Reference Application is starting!"
echo ""
echo "📊 Access Points:"
echo "  • API Service:        http://localhost:8080"
echo "  • Subscriber Service: http://localhost:8081"
echo "  • Grafana:           http://localhost:3001 (admin/admin)"
echo "  • Prometheus:        http://localhost:9090"
echo "  • Jaeger:           http://localhost:16686"
echo "  • OpenTelemetry:    http://localhost:55679"
echo ""
echo "🔍 Health Checks:"
echo "  • API Health:        http://localhost:8080/api/v1/orders/health"
echo "  • API Actuator:      http://localhost:8080/actuator/health"
echo "  • Subscriber Health: http://localhost:8081/actuator/health"
echo ""
echo "📈 Metrics Endpoints:"
echo "  • API Metrics:       http://localhost:8080/actuator/prometheus"
echo "  • Subscriber Metrics: http://localhost:8081/actuator/prometheus"
echo ""
echo "⏳ Services may take a few minutes to be fully ready..."
echo "💡 Check logs with: docker-compose logs -f [service-name]"
echo "🛑 Stop with: docker-compose down"