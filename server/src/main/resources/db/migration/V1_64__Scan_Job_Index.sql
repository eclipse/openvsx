-- Add new index for table scan_job
CREATE INDEX IF NOT EXISTS scan_job_type_status_created_idx ON scan_job(scanner_type, status, created_at);
