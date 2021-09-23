-- drop existing case-sensitive unique constraint
ALTER TABLE namespace DROP CONSTRAINT ukeq2y9mghytirkcofquanv5frf;
-- create case-insensitive unique index
CREATE UNIQUE INDEX ukeq2y9mghytirkcofquanv5frf ON namespace(UPPER(name));