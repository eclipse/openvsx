ALTER TABLE extension DROP COLUMN latest_id;
ALTER TABLE extension DROP COLUMN latest_pre_release_id;

ALTER TABLE extension_version ADD COLUMN target_platform CHARACTER VARYING(255);
UPDATE extension_version SET target_platform = 'universal';
ALTER TABLE extension_version ALTER COLUMN target_platform SET NOT NULL;

DROP INDEX unique_extension_version;
CREATE UNIQUE INDEX unique_extension_version_idx ON extension_version(extension_id, target_platform, version);
ALTER TABLE extension_version ADD CONSTRAINT unique_extension_version UNIQUE USING INDEX unique_extension_version_idx;