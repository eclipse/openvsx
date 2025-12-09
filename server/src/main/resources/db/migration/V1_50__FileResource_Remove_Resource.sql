-- don't run removed migration
UPDATE migration_item SET migration_scheduled = TRUE WHERE migration_script = 'V1_23__FileResource_Extract_Resources.sql';

INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_50__FileResource_Remove_Resource.sql', fr.id, FALSE
FROM file_resource fr
WHERE fr.type = 'resource';