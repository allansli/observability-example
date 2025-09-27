#!/bin/bash

# Start infrastructure script for observability reference application

echo "🚀 Starting Observability Reference Application Infrastructure..."
echo "=================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    echo "❌ docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

# Navigate to project directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR/.."

echo "📁 Working directory: $(pwd)"

# Create logs directory if it doesn't exist
mkdir -p logs

# Start all services
echo "🏗️  Starting all infrastructure services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to become healthy..."
sleep 30

# Check service health
echo "🔍 Checking service health..."

services=("postgres" "pubsub-emulator" "prometheus" "jaeger" "loki" "grafana" "otel-collector")
healthy_services=0

for service in "${services[@]}"; do
    if docker-compose ps -q $service > /dev/null 2>&1; then
        status=$(docker-compose ps $service | tail -n +3 | awk '{print $4}')
        if [[ $status == *"healthy"* ]] || [[ $status == *"Up"* ]]; then
            echo "✅ $service: $status"
            ((healthy_services++))
        else
            echo "❌ $service: $status"
        fi
    else
        echo "❌ $service: Not found"
    fi
done

echo "=================================================="
echo "📊 Infrastructure Status: $healthy_services/${#services[@]} services healthy"

if [ $healthy_services -eq ${#services[@]} ]; then
    echo "🎉 All services are running successfully!"
    echo ""
    echo "🌐 Access URLs:"
    echo "   • Grafana:    http://localhost:3001 (admin/admin)"
    echo "   • Jaeger:     http://localhost:16686"
    echo "   • Prometheus: http://localhost:9090"
    echo "   • Postgres:   localhost:5432 (obs_user/obs_password)"
    echo "   • Pub/Sub:    localhost:8085"
    echo ""
    echo "🔧 To stop all services:"
    echo "   docker-compose down"
    echo ""
    echo "📋 To view logs:"
    echo "   docker-compose logs -f [service-name]"
else
    echo "⚠️  Some services are not healthy. Check logs with:"
    echo "   docker-compose logs [service-name]"
fi

echo "=================================================="