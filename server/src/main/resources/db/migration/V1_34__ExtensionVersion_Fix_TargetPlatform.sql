INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_34__ExtensionVersion_Fix_TargetPlatform.sql', fr.id, FALSE
FROM file_resource fr
JOIN extension_version ev ON ev.id = fr.extension_id
JOIN extension e ON e.id = ev.extension_id
WHERE fr.type = 'download' AND ev.target_platform = 'universal'
ORDER BY e.download_count DESC;