-- Add index for fast blocklist lookups during extension publishing
-- Optimizes BlocklistCheckService.findBlockedByFileHashIn() query:
--   SELECT f FROM FileDecision f WHERE f.fileHash IN :fileHashes AND f.decision = 'BLOCKED'

-- Composite index: makes decision a covering column for file_hash lookups
-- PostgreSQL can check the decision filter directly from the index without table access
CREATE INDEX IF NOT EXISTS idx_file_decision_hash_decision 
    ON file_decision(file_hash, decision);
