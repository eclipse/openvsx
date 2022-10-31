CREATE TABLE public.set_pre_release_migration_item (
    id bigint NOT NULL,
    extension_id bigint NOT NULL,
    migration_scheduled boolean NOT NULL
);

ALTER TABLE ONLY public.set_pre_release_migration_item
ADD CONSTRAINT set_pre_release_migration_item_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX set_pre_release_migration_item_extension_id
ON set_pre_release_migration_item (extension_id);

ALTER TABLE set_pre_release_migration_item
ADD CONSTRAINT set_pre_release_migration_item_unique_extension_id
UNIQUE USING INDEX set_pre_release_migration_item_extension_id;

INSERT INTO set_pre_release_migration_item(id, extension_id, migration_scheduled)
SELECT nextval('hibernate_sequence'), id, FALSE FROM extension;