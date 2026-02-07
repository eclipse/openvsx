-- Consolidated migration for scanning infrastructure
-- Combines: scan_job table, scan_check_result table, and extension_threat updates

-- ============================================================================
-- Part 1: Create scan_job table
-- Tracks the status of scanner jobs for extension versions
-- ============================================================================

CREATE TABLE IF NOT EXISTS scan_job (
    id BIGINT NOT NULL,
    scan_id CHARACTER VARYING(255) NOT NULL,
    scanner_type CHARACTER VARYING(255) NOT NULL,
    extension_version_id BIGINT NOT NULL,
    external_job_id CHARACTER VARYING(512),
    status CHARACTER VARYING(20) NOT NULL,
    recovery_in_progress BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    error_message CHARACTER VARYING(2048),
    -- Lease-based polling columns (crash-safe distributed locking)
    poll_lease_until TIMESTAMP WITHOUT TIME ZONE,
    poll_attempts INTEGER DEFAULT 0 NOT NULL,
    -- File hash mapping for async scanners with file extraction
    file_hashes_json TEXT,
    CONSTRAINT scan_job_pkey PRIMARY KEY (id)
);

-- Add column comments
COMMENT ON COLUMN scan_job.poll_lease_until IS 
'Lease-based polling lock. When null: available for polling. When set: being polled until this timestamp. Crash-safe: expired leases can be retaken.';

COMMENT ON COLUMN scan_job.poll_attempts IS 
'Number of times this job has been polled (for async scanners). Used to detect stuck jobs and enforce max poll attempts.';

COMMENT ON COLUMN scan_job.file_hashes_json IS 
'Stores filename→hash mapping for async scanners with file extraction. Used to look up hashes when results come back later.';

-- Create indexes for scan_job
CREATE INDEX IF NOT EXISTS scan_job_scan_id_idx ON scan_job(scan_id);
CREATE INDEX IF NOT EXISTS scan_job_status_idx ON scan_job(status);
CREATE INDEX IF NOT EXISTS scan_job_extension_version_idx ON scan_job(extension_version_id);
CREATE INDEX IF NOT EXISTS scan_job_poll_lease_idx ON scan_job(poll_lease_until);

-- Create unique constraint for scan_job
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'scan_job_scan_id_scanner_type_key'
    ) THEN
        ALTER TABLE public.scan_job
            ADD CONSTRAINT scan_job_scan_id_scanner_type_key UNIQUE (scan_id, scanner_type);
    END IF;
END $$;

-- Create sequence for scan_job
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'scan_job_seq' AND relkind = 'S') THEN
        CREATE SEQUENCE scan_job_seq INCREMENT 50;
    ELSE
        ALTER SEQUENCE scan_job_seq INCREMENT 50;
    END IF;
    ALTER SEQUENCE scan_job_seq OWNED BY public.scan_job.id;
END $$;

-- Set sequence to max existing ID + 1
SELECT SETVAL('scan_job_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM scan_job)::BIGINT);


-- ============================================================================
-- Part 2: Create scan_check_result table
-- Records all check/scan executions for audit trail (even passing checks)
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS scan_check_result_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS scan_check_result (
    id BIGINT NOT NULL DEFAULT nextval('scan_check_result_seq'),
    scan_id BIGINT NOT NULL,
    check_type VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    result VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    files_scanned INTEGER,
    findings_count INTEGER,
    summary VARCHAR(512),
    error_message VARCHAR(2048),
    scanner_job_id BIGINT,
    required BOOLEAN,
    PRIMARY KEY (id),
    CONSTRAINT fk_scan_check_result_scan FOREIGN KEY (scan_id) 
        REFERENCES extension_scan(id) ON DELETE CASCADE
);

-- Create indexes for scan_check_result
CREATE INDEX IF NOT EXISTS scan_check_result_scan_id_idx ON scan_check_result(scan_id);
CREATE INDEX IF NOT EXISTS scan_check_result_check_type_idx ON scan_check_result(check_type);

-- Add table and column comments
COMMENT ON TABLE scan_check_result IS 'Records all check/scan executions for audit trail';
COMMENT ON COLUMN scan_check_result.check_type IS 'Type of check: BLOCKLIST, SECRET, NAME_SQUATTING, CLAMAV_REST, etc.';
COMMENT ON COLUMN scan_check_result.category IS 'PUBLISH_CHECK (sync) or SCANNER_JOB (async/sync scanner)';
COMMENT ON COLUMN scan_check_result.result IS 'PASSED, FAILED, WARNING, ERROR, or SKIPPED';
COMMENT ON COLUMN scan_check_result.summary IS 'Brief description like "Scanned 348 files, no threats found"';
COMMENT ON COLUMN scan_check_result.required IS 'Whether check errors block publishing';


-- ============================================================================
-- Part 3: Update extension_threat table
-- Add job_id and allow nullable file fields for package-level scanners
-- ============================================================================

-- Add job_id column to link threats to the scan job that detected them
ALTER TABLE extension_threat ADD COLUMN IF NOT EXISTS job_id BIGINT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_threat_job_id ON extension_threat(job_id);

-- Allow null values for file_name and file_hash
-- Some scanners scan the whole package and don't report individual files.
-- Using null instead of placeholder values prevents them from being added to allow/block lists.
ALTER TABLE extension_threat ALTER COLUMN file_name DROP NOT NULL;
ALTER TABLE extension_threat ALTER COLUMN file_hash DROP NOT NULL;
