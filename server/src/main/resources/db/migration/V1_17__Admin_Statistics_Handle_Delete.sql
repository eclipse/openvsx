-- also insert row into entity_active_state when entity is deleted
CREATE OR REPLACE FUNCTION public.insert_entity_active_state() RETURNS TRIGGER AS $entity_active_state_trigger$
    DECLARE
		id BIGINT;
		active BOOLEAN;
    BEGIN
        IF TG_OP = 'DELETE' THEN
			id := OLD.id;
			active := FALSE;
        ELSE
			id := NEW.id;
			active := NEW.active;
        END IF;
        INSERT INTO entity_active_state(id, entity_id, entity_type, active, "timestamp")
        VALUES(nextval('entity_active_state_id_seq'), id, TG_TABLE_NAME, active, CURRENT_TIMESTAMP);
        RETURN NULL;
    END;
$entity_active_state_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER extension_delete_entity_active_state
AFTER DELETE ON extension
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_review_delete_entity_active_state
AFTER DELETE ON extension_review
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_version_delete_entity_active_state
AFTER DELETE ON extension_version
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER personal_access_token_delete_entity_active_state
AFTER DELETE ON personal_access_token
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

-- insert rows into entity_active_state for deleted entities
-- that are still marked as active in the entity_active_state table
INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), d.entity_id, 'extension', FALSE, CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT s.entity_id
    FROM entity_active_state s
    LEFT JOIN extension e ON e.id = s.entity_id
    WHERE e.id IS NULL AND s.entity_type = 'extension' AND s.active = TRUE
) d;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), d.entity_id, 'extension_review', FALSE, CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT s.entity_id
    FROM entity_active_state s
    LEFT JOIN extension_review r ON r.id = s.entity_id
    WHERE r.id IS NULL AND s.entity_type = 'extension_review' AND s.active = TRUE
) d;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), d.entity_id, 'extension_version', FALSE, CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT s.entity_id
    FROM entity_active_state s
    LEFT JOIN extension_version v ON v.id = s.entity_id
    WHERE v.id IS NULL AND s.entity_type = 'extension_version' AND s.active = TRUE
) d;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), d.entity_id, 'personal_access_token', FALSE, CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT s.entity_id
    FROM entity_active_state s
    LEFT JOIN personal_access_token t ON t.id = s.entity_id
    WHERE t.id IS NULL AND s.entity_type = 'personal_access_token' AND s.active = TRUE
) d;

-- keep downloads that refer to deleted file resource
ALTER TABLE ONLY public.download DROP CONSTRAINT download_file_resource_fkey;
ALTER TABLE ONLY public.download RENAME COLUMN file_resource_id TO file_resource_id_not_fk;