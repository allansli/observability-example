#!/bin/bash

set -e

API_URL="http://localhost:8080/api/v1/orders"

echo "🧪 Testing Observability Reference Application API..."

# Function to create a random order
create_order() {
    local customer_id="customer-$(shuf -i 1-100 -n 1)"
    local products=("Laptop Pro" "Wireless Mouse" "USB Cable" "Monitor 4K" "Keyboard Mechanical" "Webcam HD" "Speakers" "Headphones")
    local product="${products[$RANDOM % ${#products[@]}]}"
    local quantity=$(shuf -i 1-5 -n 1)
    local unit_price=$(printf "%.2f" $(echo "scale=2; $RANDOM/100" | bc))
    
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"$customer_id\",
            \"productName\": \"$product\",
            \"quantity\": $quantity,
            \"unitPrice\": $unit_price
        }" | jq '.'
}

# Function to get order by ID
get_order() {
    local order_id=$1
    echo "📋 Getting order: $order_id"
    curl -s "$API_URL/$order_id" | jq '.'
}

# Function to get orders by customer
get_orders_by_customer() {
    local customer_id=$1
    echo "👤 Getting orders for customer: $customer_id"
    curl -s "$API_URL?customerId=$customer_id" | jq '.'
}

# Check if API is running
echo "🔍 Checking API health..."
if ! curl -f "$API_URL/health" &>/dev/null; then
    echo "❌ API is not responding. Make sure the application is running."
    echo "💡 Run: ./scripts/start.sh"
    exit 1
fi
echo "✅ API is healthy!"

echo ""
echo "📊 Creating sample orders for observability testing..."

# Create multiple orders to generate observability data
for i in {1..10}; do
    echo "🛒 Creating order $i..."
    order_response=$(create_order)
    order_id=$(echo "$order_response" | jq -r '.orderId')
    
    echo "Order ID: $order_id"
    
    # Add some delay to simulate realistic traffic
    sleep 1
done

echo ""
echo "⏳ Waiting 5 seconds for message processing..."
sleep 5

echo ""
echo "🔍 Testing order retrieval..."

# Test getting a specific order
echo "📋 Testing get order by ID..."
order_id=$(create_order | jq -r '.orderId')
get_order "$order_id"

echo ""
echo "👤 Testing get orders by customer..."
get_orders_by_customer "customer-1"

echo ""
echo "📈 Generating load for observability metrics..."

# Generate some load to create interesting metrics
for i in {1..20}; do
    create_order > /dev/null &
    
    # Occasionally make a bad request to generate error metrics
    if [ $((i % 7)) -eq 0 ]; then
        curl -s -X POST "$API_URL" \
            -H "Content-Type: application/json" \
            -d "{\"invalid\": \"data\"}" > /dev/null &
    fi
    
    # Add some variation in timing
    if [ $((i % 3)) -eq 0 ]; then
        sleep 0.1
    fi
done

# Wait for background requests to complete
wait

echo ""
echo "🎉 API testing completed!"
echo ""
echo "📊 Check observability data in:"
echo "  • Grafana:    http://localhost:3001"
echo "  • Prometheus: http://localhost:9090"
echo "  • Jaeger:     http://localhost:16686"
echo ""
echo "💡 Sample traces should be visible in Jaeger"
echo "💡 Metrics should be visible in Prometheus and Grafana"
echo "💡 Logs should be visible in Grafana (Loki datasource)"