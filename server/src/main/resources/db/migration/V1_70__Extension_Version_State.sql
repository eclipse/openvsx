ALTER TABLE extension_version ADD COLUMN state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
UPDATE extension_version SET state = 'INACTIVE' WHERE active = false;

ALTER TABLE extension_version ADD COLUMN last_updated TIMESTAMP;
UPDATE extension_version SET last_updated = timestamp;
ALTER TABLE extension_version ALTER COLUMN last_updated SET NOT NULL;
