ALTER TABLE file_resource ADD COLUMN content_type CHARACTER VARYING(255);
UPDATE file_resource SET content_type = 'text/plain' WHERE type = 'sha256' OR type = 'signature';
UPDATE file_resource SET content_type = 'application/zip' WHERE type = 'download';

INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_41__FileResource_ContentType.sql', ev.id, FALSE
FROM extension_version ev
JOIN extension e ON e.id = ev.extension_id
ORDER BY e.download_count DESC;