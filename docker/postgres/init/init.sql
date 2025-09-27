-- Initialize the orders database with proper schema

-- Create orders table with proper indexes
CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19,2) NOT NULL CHECK (unit_price > 0),
    total_amount DECIMAL(19,2) NOT NULL CHECK (total_amount > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_processed_at ON orders(processed_at);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders(customer_id, status);

-- Insert sample data for testing
INSERT INTO orders (order_id, customer_id, product_name, quantity, unit_price, total_amount, status, created_at) 
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'customer-001', 'Laptop Pro', 1, 1299.99, 1299.99, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    ('550e8400-e29b-41d4-a716-446655440002', 'customer-002', 'Wireless Mouse', 2, 29.99, 59.98, 'PROCESSED', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    ('550e8400-e29b-41d4-a716-446655440003', 'customer-001', 'USB Cable', 3, 9.99, 29.97, 'PROCESSED', CURRENT_TIMESTAMP - INTERVAL '15 minutes')
ON CONFLICT (order_id) DO NOTHING;

-- Create a view for order analytics
CREATE OR REPLACE VIEW order_analytics AS
SELECT 
    customer_id,
    COUNT(*) as total_orders,
    SUM(total_amount) as total_spent,
    AVG(total_amount) as avg_order_value,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_orders,
    COUNT(CASE WHEN status = 'PROCESSED' THEN 1 END) as processed_orders,
    MIN(created_at) as first_order_date,
    MAX(created_at) as last_order_date
FROM orders 
GROUP BY customer_id;