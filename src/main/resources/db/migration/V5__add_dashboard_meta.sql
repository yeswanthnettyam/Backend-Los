-- Add dashboard_meta column to flow_configs table
-- This column stores UI metadata (title, description, icon) for dashboard rendering

ALTER TABLE FLOW_CONFIGS ADD COLUMN DASHBOARD_META TEXT;
