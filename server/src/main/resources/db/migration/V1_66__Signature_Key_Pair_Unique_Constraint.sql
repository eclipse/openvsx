-- create a unique index to ensure only 1 active signature key pair is present
CREATE UNIQUE INDEX IF NOT EXISTS unique_active_signature_key_pair_idx on signature_key_pair (active) where active;
