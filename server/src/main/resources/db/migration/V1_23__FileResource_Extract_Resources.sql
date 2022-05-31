CREATE TABLE public.extract_resources_migration_item (
    id bigint NOT NULL,
    extension_id bigint NOT NULL,
    migration_scheduled boolean NOT NULL
);

ALTER TABLE ONLY public.extract_resources_migration_item
ADD CONSTRAINT extract_resources_migration_item_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX extract_resources_migration_item_extension_id
ON extract_resources_migration_item (extension_id);

ALTER TABLE extract_resources_migration_item
ADD CONSTRAINT unique_extension_id
UNIQUE USING INDEX extract_resources_migration_item_extension_id;

INSERT INTO extract_resources_migration_item(id, extension_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), id, FALSE FROM extension_version;