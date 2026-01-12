-- Seed Master Data
-- This script is idempotent - it only inserts data if it doesn't already exist

-- Insert Partners (only if they don't exist)
MERGE INTO partners (partner_code, partner_name, is_active, created_at)
KEY (partner_code)
VALUES
('SAMASTA', 'Samasta Microfinance', TRUE, CURRENT_TIMESTAMP),
('SONATA', 'Sonata Finance', TRUE, CURRENT_TIMESTAMP);

-- Insert Products (only if they don't exist)
MERGE INTO products (product_code, product_name, is_active, created_at)
KEY (product_code)
VALUES
('ENTREPRENEURIAL', 'Entrepreneurial Loan', TRUE, CURRENT_TIMESTAMP),
('JLG', 'Joint Liability Group Loan', TRUE, CURRENT_TIMESTAMP);

