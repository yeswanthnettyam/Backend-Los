-- Uploaded Files table for camera uploads
CREATE TABLE uploaded_files (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    screen_id VARCHAR(100) NOT NULL,
    field_id VARCHAR(100) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_url VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id)
);

CREATE INDEX idx_uploaded_files_app_id ON uploaded_files(application_id);
CREATE INDEX idx_uploaded_files_screen_field ON uploaded_files(screen_id, field_id);
CREATE INDEX idx_uploaded_files_app_screen ON uploaded_files(application_id, screen_id);
