INSERT INTO migration_item(id, job_name, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'FixMissingFilesMigration', id, FALSE
FROM extension_version;