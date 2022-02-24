-- add latest pre-release column to extension table
ALTER TABLE extension ADD COLUMN latest_pre_release_id BIGINT;
ALTER TABLE extension ADD CONSTRAINT extension_latest_pre_release_fkey FOREIGN KEY (latest_pre_release_id) REFERENCES extension_version(id);
ALTER TABLE extension DROP COLUMN preview_id;

-- add pre-release column to extension_version table
ALTER TABLE extension_version ADD COLUMN pre_release BOOL;
UPDATE extension_version SET pre_release = FALSE;
ALTER TABLE extension_version ALTER COLUMN pre_release SET NOT NULL;