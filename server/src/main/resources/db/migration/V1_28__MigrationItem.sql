-- create generic migration_item table
CREATE TABLE migration_item (
    id bigint NOT NULL,
    migration_script character varying(1000) NOT NULL,
    entity_id bigint NOT NULL,
    migration_scheduled boolean NOT NULL
);

ALTER TABLE ONLY public.migration_item
ADD CONSTRAINT migration_item_pkey PRIMARY KEY (id);

-- move extract_resources_migration_item to migration_item
INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_23__FileResource_Extract_Resources.sql', mi.extension_id, mi.migration_scheduled
FROM extract_resources_migration_item mi
LEFT JOIN extension_version ev ON ev.id = mi.extension_id
LEFT JOIN extension e ON e.id = ev.extension_id
ORDER BY e.download_count DESC;

DROP TABLE extract_resources_migration_item;

-- move set_pre_release_migration_item to migration_item
INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_26__Extension_Set_PreRelease.sql', mi.extension_id, mi.migration_scheduled
FROM set_pre_release_migration_item mi
LEFT JOIN extension e ON e.id = mi.extension_id
ORDER BY e.download_count DESC;

DROP TABLE set_pre_release_migration_item;

-- insert rename downloads migrations
INSERT INTO migration_item(id, migration_script, entity_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), 'V1_28__MigrationItem.sql', fr.id, FALSE
FROM file_resource fr
JOIN extension_version ev ON ev.id = fr.extension_id
JOIN extension e ON e.id = ev.extension_id
WHERE fr.type = 'download'
ORDER BY e.download_count DESC;
