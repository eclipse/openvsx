ALTER TABLE namespace ADD COLUMN logo_content_type CHARACTER VARYING(255);

INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_42__Namespace_Logo_ContentType.sql', n.id, FALSE
FROM namespace n
WHERE n.logo_storage_type IS NOT NULL;