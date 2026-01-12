-- Repeatable Master Data Migration
-- This script runs every time it changes (similar to Liquibase)
-- Use MERGE to ensure idempotency - data is only inserted if it doesn't exist, updated if it does

-- Maintain Partners Master Data
MERGE INTO partners (partner_code, partner_name, is_active, created_at)
KEY (partner_code)
VALUES
('SAMASTA', 'Samasta Microfinance', TRUE, CURRENT_TIMESTAMP),
('SONATA', 'Sonata Finance', TRUE, CURRENT_TIMESTAMP);

-- Maintain Products Master Data
MERGE INTO products (product_code, product_name, is_active, created_at)
KEY (product_code)
VALUES
('ENTREPRENEURIAL', 'Entrepreneurial Loan', TRUE, CURRENT_TIMESTAMP),
('JLG', 'Joint Liability Group Loan', TRUE, CURRENT_TIMESTAMP);

-- Add new master data below as needed
-- Example:
-- MERGE INTO partners (partner_code, partner_name, is_active, created_at)
-- KEY (partner_code)
-- VALUES ('NEW_PARTNER', 'New Partner Name', TRUE, CURRENT_TIMESTAMP);
