-- Configuration Tables

-- Screen Configs table
CREATE TABLE screen_configs (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(50),
    partner_code VARCHAR(50),
    branch_code VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    ui_config TEXT NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    lock_version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_screen_config UNIQUE (screen_id, product_code, partner_code, branch_code, version)
);

CREATE INDEX idx_screen_config_lookup ON screen_configs(screen_id, product_code, partner_code, branch_code, status);

-- Validation Configs table
CREATE TABLE validation_configs (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(50),
    partner_code VARCHAR(50),
    branch_code VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    validation_rules TEXT NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    lock_version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_validation_config UNIQUE (screen_id, product_code, partner_code, branch_code, version)
);

CREATE INDEX idx_validation_config_lookup ON validation_configs(screen_id, product_code, partner_code, branch_code, status);

-- Field Mapping Configs table
CREATE TABLE field_mapping_configs (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(50),
    partner_code VARCHAR(50),
    branch_code VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    mappings TEXT NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    lock_version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_field_mapping_config UNIQUE (screen_id, product_code, partner_code, branch_code, version)
);

CREATE INDEX idx_field_mapping_config_lookup ON field_mapping_configs(screen_id, product_code, partner_code, branch_code, status);

-- Flow Configs table
CREATE TABLE flow_configs (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(50),
    partner_code VARCHAR(50),
    branch_code VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    flow_definition TEXT NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    lock_version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_flow_config UNIQUE (flow_id, product_code, partner_code, branch_code, version)
);

CREATE INDEX idx_flow_config_lookup ON flow_configs(flow_id, product_code, partner_code, branch_code, status);

-- Flow Snapshots table
CREATE TABLE flow_snapshots (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    flow_config_id BIGINT NOT NULL,
    snapshot_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id),
    FOREIGN KEY (flow_config_id) REFERENCES flow_configs(config_id)
);

CREATE INDEX idx_flow_snapshot_app_id ON flow_snapshots(application_id);

