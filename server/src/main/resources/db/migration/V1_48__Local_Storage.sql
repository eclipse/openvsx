INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_48__Local_Storage_FileResource.sql', fr.id, FALSE
FROM file_resource fr
JOIN extension_version ev ON ev.id = fr.extension_id
JOIN extension e ON e.id = ev.extension_id
WHERE fr.storage_type = 'database'
ORDER BY e.download_count DESC;

INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_48__Local_Storage_Namespace.sql', n.id, FALSE
FROM namespace n
WHERE n.logo_storage_type = 'database';

UPDATE file_resource SET storage_type = 'local' WHERE storage_type = 'database';
UPDATE  namespace SET logo_storage_type = 'local' WHERE logo_storage_type = 'database';
