-- Track the size (in bytes) of each file resource
ALTER TABLE file_resource ADD COLUMN IF NOT EXISTS size BIGINT;

-- Backfill the size of existing file resources, most downloaded extensions first
INSERT INTO migration_item(id, job_name, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'FileResourceSizeMigration', fr.id, FALSE
FROM file_resource fr
JOIN extension_version ev ON ev.id = fr.extension_id
JOIN extension e ON e.id = ev.extension_id
ORDER BY e.download_count DESC;
