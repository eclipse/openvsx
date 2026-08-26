-- Track the size (in bytes) of each file resource
ALTER TABLE file_resource ADD COLUMN IF NOT EXISTS size BIGINT;

-- Backfill the size of existing file resources, most downloaded extensions first
INSERT INTO migration_item(id, job_name, entity_id, migration_scheduled)
SELECT nextval('migration_item_seq'), 'FileResourceSizeMigration', ev.id, FALSE
FROM extension_version ev
         JOIN extension e ON e.id = ev.extension_id
WHERE EXISTS (SELECT 1 FROM file_resource fr WHERE fr.extension_id = ev.id AND fr.size IS NULL)
ORDER BY e.download_count DESC;
