-- Create tables for extension scanning and validation system
-- These tables track the lifecycle of extension validation and malware scanning

-- ============================================================================
-- SEQUENCES
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS extension_scan_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS extension_validation_failure_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS extension_threat_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS admin_scan_decision_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS file_decision_seq START WITH 1 INCREMENT BY 1;

-- ============================================================================
-- EXTENSION_SCAN TABLE
-- ============================================================================

-- Main scan record table
-- Tracks complete lifecycle from upload through validation, scanning, and admin review
-- Uses raw string values (not foreign keys) to preserve scan history even if extension is deleted
CREATE TABLE extension_scan (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('extension_scan_seq'),
    
    -- Raw metadata about what was scanned (preserved even if extension deleted)
    namespace_name VARCHAR(255) NOT NULL,
    extension_name VARCHAR(255) NOT NULL,
    extension_version VARCHAR(100) NOT NULL,
    target_platform VARCHAR(255) NOT NULL,
    extension_display_name VARCHAR(255),
    universal_target_platform BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Publisher information (user who published the extension)
    publisher VARCHAR(255) NOT NULL,
    publisher_url VARCHAR(255),
    
    -- Timestamps for tracking scan lifecycle
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    
    -- Current status of the scan process
    status VARCHAR(20) NOT NULL,
    
    -- Error message if scan encountered an error (null otherwise)
    error_message VARCHAR(2048)
);

-- ============================================================================
-- EXTENSION_VALIDATION_FAILURE TABLE
-- ============================================================================

-- Validation failures table
-- Records why an extension failed fast-fail validation checks
CREATE TABLE extension_validation_failure (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('extension_validation_failure_seq'),
    
    -- Foreign key to parent scan
    scan_id BIGINT NOT NULL,
    
    -- Type of validation check that failed
    validation_type VARCHAR(100) NOT NULL,
    
    -- Name of the specific validation rule that failed
    rule_name VARCHAR(255) NOT NULL,
    
    -- Detailed explanation of why the validation failed
    validation_failure_reason VARCHAR(1024) NOT NULL,

    -- Whether this failure was enforced at the time it was detected.
    enforced BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamp when the validation failure was detected
    detected_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_validation_failure_scan FOREIGN KEY (scan_id) 
        REFERENCES extension_scan(id) ON DELETE CASCADE
);

-- ============================================================================
-- EXTENSION_THREAT TABLE
-- ============================================================================

-- Threat detection results from security scanners
-- Each row represents one file flagged by one scanner
CREATE TABLE extension_threat (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('extension_threat_seq'),
    
    -- Foreign key to parent scan
    scan_id BIGINT NOT NULL,
    
    -- File information
    file_name VARCHAR(1024) NOT NULL,
    file_hash VARCHAR(128) NOT NULL,
    file_extension VARCHAR(50),
    
    -- Scanner information
    scanner_type VARCHAR(100) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    reason VARCHAR(2048),
    severity VARCHAR(50),
    
    -- Whether this threat is enforced
    enforced BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamp when threat was detected
    detected_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_threat_scan FOREIGN KEY (scan_id)
        REFERENCES extension_scan(id) ON DELETE CASCADE
);

-- ============================================================================
-- ADMIN_SCAN_DECISION TABLE
-- ============================================================================

-- Admin decisions on quarantined scans (allow/block)
-- Only one decision per scan
CREATE TABLE admin_scan_decision (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('admin_scan_decision_seq'),
    
    -- Foreign key to parent scan (unique - one decision per scan)
    scan_id BIGINT NOT NULL UNIQUE,
    
    -- Decision: ALLOWED or BLOCKED
    decision VARCHAR(20) NOT NULL,
    
    -- Admin who made the decision (foreign key to user_data)
    decided_by_id BIGINT NOT NULL,
    
    -- When the decision was made
    decided_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_decision_scan FOREIGN KEY (scan_id)
        REFERENCES extension_scan(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_scan_decision_user FOREIGN KEY (decided_by_id)
        REFERENCES user_data(id),
    
    -- Ensure valid decision values
    CONSTRAINT chk_decision_value CHECK (decision IN ('ALLOWED', 'BLOCKED'))
);

-- ============================================================================
-- FILE_DECISION TABLE
-- ============================================================================

-- Allow list / block list for individual files (by hash)
-- Used to approve or block specific file content for all future extension scans
CREATE TABLE file_decision (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('file_decision_seq'),
    
    -- File identification
    file_hash VARCHAR(128) NOT NULL UNIQUE,
    file_name VARCHAR(1024),
    file_type VARCHAR(50),
    
    -- Decision: ALLOWED or BLOCKED
    decision VARCHAR(20) NOT NULL,
    
    -- Admin who made the decision (foreign key to user_data)
    decided_by_id BIGINT NOT NULL,
    
    -- When the decision was made
    decided_at TIMESTAMP NOT NULL,
    
    -- Context information
    -- These capture the extension where the file was first encountered
    display_name VARCHAR(255),
    namespace_name VARCHAR(255),
    extension_name VARCHAR(255),
    publisher VARCHAR(255),
    version VARCHAR(100),
    
    -- Optional link to the scan that triggered this decision
    scan_id BIGINT,
    
    -- Foreign key constraints
    CONSTRAINT fk_file_decision_scan FOREIGN KEY (scan_id)
        REFERENCES extension_scan(id) ON DELETE SET NULL,
    CONSTRAINT fk_file_decision_user FOREIGN KEY (decided_by_id)
        REFERENCES user_data(id),
    
    -- Ensure valid decision values
    CONSTRAINT chk_file_decision_value CHECK (decision IN ('ALLOWED', 'BLOCKED'))
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Indexes for extension_scan
CREATE INDEX idx_extension_scan_version ON extension_scan(namespace_name, extension_name, extension_version, target_platform);
CREATE INDEX idx_extension_scan_status ON extension_scan(status);
CREATE INDEX idx_extension_scan_completed_at ON extension_scan(completed_at);
CREATE INDEX idx_extension_scan_started_at ON extension_scan(started_at DESC);

-- Indexes for extension_validation_failure
CREATE INDEX idx_validation_failure_scan ON extension_validation_failure(scan_id);
CREATE INDEX idx_validation_failure_validation_type ON extension_validation_failure(validation_type);
CREATE INDEX idx_validation_failure_detected_at ON extension_validation_failure(detected_at);

-- Indexes for extension_threat
CREATE INDEX idx_threat_scan ON extension_threat(scan_id);
CREATE INDEX idx_threat_scanner_type ON extension_threat(scanner_type);
CREATE INDEX idx_threat_file_hash ON extension_threat(file_hash);
CREATE INDEX idx_threat_detected_at ON extension_threat(detected_at);

-- Indexes for admin_scan_decision
CREATE INDEX idx_scan_decision_decided_at ON admin_scan_decision(decided_at);
CREATE INDEX idx_scan_decision_decision ON admin_scan_decision(decision);
CREATE INDEX idx_scan_decision_decided_by ON admin_scan_decision(decided_by_id);

-- Indexes for file_decision
CREATE INDEX idx_file_decision_decided_at ON file_decision(decided_at);
CREATE INDEX idx_file_decision_decision ON file_decision(decision);
CREATE INDEX idx_file_decision_namespace ON file_decision(namespace_name);
CREATE INDEX idx_file_decision_publisher ON file_decision(publisher);
CREATE INDEX idx_file_decision_decided_by ON file_decision(decided_by_id);

-- Composite index for fast blocklist lookups during extension publishing
-- Optimizes BlocklistCheckService.findBlockedByFileHashIn() query
-- PostgreSQL can check the decision filter directly from the index without table access
CREATE INDEX idx_file_decision_hash_decision ON file_decision(file_hash, decision);