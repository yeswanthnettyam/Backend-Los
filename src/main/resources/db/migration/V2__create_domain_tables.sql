-- Domain Tables

-- Loan Applications table
CREATE TABLE loan_applications (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    partner_code VARCHAR(50) NOT NULL,
    branch_code VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    current_screen_id VARCHAR(100),
    flow_snapshot_id BIGINT,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (product_code) REFERENCES products(product_code),
    FOREIGN KEY (partner_code) REFERENCES partners(partner_code)
);

CREATE INDEX idx_loan_app_product ON loan_applications(product_code);
CREATE INDEX idx_loan_app_partner ON loan_applications(partner_code);
CREATE INDEX idx_loan_app_status ON loan_applications(status);

-- Applicants table
CREATE TABLE applicants (
    applicant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    first_name VARCHAR(100),
    middle_name VARCHAR(100),
    last_name VARCHAR(100),
    mobile VARCHAR(15),
    email VARCHAR(255),
    dob DATE,
    gender VARCHAR(20),
    pan_number VARCHAR(10),
    aadhaar_number VARCHAR(12),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id)
);

CREATE INDEX idx_applicant_app_id ON applicants(application_id);
CREATE INDEX idx_applicant_mobile ON applicants(mobile);
CREATE INDEX idx_applicant_pan ON applicants(pan_number);

-- Businesses table
CREATE TABLE businesses (
    business_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    business_name VARCHAR(255),
    business_type VARCHAR(100),
    business_address VARCHAR(500),
    business_vintage_months INT,
    annual_turnover DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id)
);

CREATE INDEX idx_business_app_id ON businesses(application_id);

-- Verification Records table
CREATE TABLE verification_records (
    verification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    field_id VARCHAR(100) NOT NULL,
    verification_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    response_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id)
);

CREATE INDEX idx_verification_app_id ON verification_records(application_id);
CREATE INDEX idx_verification_field_id ON verification_records(field_id);

