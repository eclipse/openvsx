-- Create tables and constraints for AdminStatistics entity
CREATE TABLE public.admin_statistics(
    id bigint NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    extensions BIGINT NOT NULL,
    downloads BIGINT NOT NULL,
    downloads_total BIGINT NOT NULL,
    publishers BIGINT NOT NULL,
    average_reviews_per_extension DOUBLE PRECISION NOT NULL,
    namespace_owners BIGINT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics ADD CONSTRAINT admin_statistics_pkey PRIMARY KEY (id);

CREATE TABLE public.admin_statistics_publishers_by_extensions_published(
    admin_statistics_id BIGINT NOT NULL,
    extensions_published INT NOT NULL,
    publishers INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_publishers_by_extensions_published
    ADD CONSTRAINT admin_statistics_publishers_by_extensions_published_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

CREATE TABLE public.admin_statistics_extensions_by_rating(
    admin_statistics_id BIGINT NOT NULL,
    rating INT NOT NULL,
    extensions INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_extensions_by_rating
    ADD CONSTRAINT admin_statistics_extensions_by_rating_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

-- Keep track of when an entity's active property changed
-- This applies to extension, extension_review, extension_version and personal_access_token tables
-- entity_active_state is used to be able to look back in time for active entities
CREATE TABLE public.entity_active_state(
    id bigint NOT NULL,
    entity_id bigint NOT NULL,
    entity_type character varying(255) NOT NULL,
    active boolean NOT NULL,
    "timestamp" timestamp without time zone
);

ALTER TABLE ONLY public.entity_active_state ADD CONSTRAINT entity_active_state_pkey PRIMARY KEY (id);

CREATE SEQUENCE entity_active_state_id_seq OWNED BY public.entity_active_state.id;

CREATE OR REPLACE FUNCTION public.insert_entity_active_state() RETURNS TRIGGER AS $entity_active_state_trigger$
    BEGIN
        INSERT INTO entity_active_state(id, entity_id, entity_type, active, "timestamp")
        VALUES(nextval('entity_active_state_id_seq'), NEW.id, TG_TABLE_NAME, NEW.active, CURRENT_TIMESTAMP);
        RETURN NULL;
    END;
$entity_active_state_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER extension_active_insert
AFTER INSERT ON extension
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_review_active_insert
AFTER INSERT ON extension_review
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_version_active_insert
AFTER INSERT ON extension_version
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER personal_access_token_active_insert
AFTER INSERT ON personal_access_token
FOR EACH ROW
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_active_update
AFTER UPDATE ON extension
FOR EACH ROW
WHEN (OLD.active <> NEW.active)
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_review_active_update
AFTER UPDATE ON extension_review
FOR EACH ROW
WHEN (OLD.active <> NEW.active)
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER extension_version_active_update
AFTER UPDATE ON extension_version
FOR EACH ROW
WHEN (OLD.active <> NEW.active)
EXECUTE PROCEDURE insert_entity_active_state();

CREATE TRIGGER personal_access_token_active_update
AFTER UPDATE ON personal_access_token
FOR EACH ROW
WHEN (OLD.active <> NEW.active)
EXECUTE PROCEDURE insert_entity_active_state();

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), e.id, 'extension', e.active, CURRENT_TIMESTAMP
FROM public.extension e;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), er.id, 'extension_review', er.active, CURRENT_TIMESTAMP
FROM public.extension_review er;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), ev.id, 'extension_version', ev.active, CURRENT_TIMESTAMP
FROM public.extension_version ev;

INSERT INTO public.entity_active_state
SELECT nextval('entity_active_state_id_seq'), t.id, 'personal_access_token', t.active, CURRENT_TIMESTAMP
FROM public.personal_access_token t;

-- keep track of when file resources were downloaded
CREATE TABLE public.download(
    id BIGINT NOT NULL,
    file_resource_id BIGINT NOT NULL,
    "timestamp" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount INT NOT NULL
);

ALTER TABLE ONLY public.download ADD CONSTRAINT download_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.download ADD CONSTRAINT download_file_resource_fkey FOREIGN KEY (file_resource_id) REFERENCES file_resource(id);

CREATE INDEX download_timestamp_brin_idx ON public.download USING BRIN(timestamp);
CREATE SEQUENCE download_id_seq OWNED BY public.download.id;

INSERT INTO public.download(id, file_resource_id, "timestamp", amount)
SELECT nextval('download_id_seq'), fr.id, CURRENT_TIMESTAMP, e.download_count
FROM extension e
JOIN extension_version ev ON ev.id = e.latest_id
JOIN file_resource fr ON fr.extension_id = ev.id
WHERE fr.type = 'download' AND e.download_count > 0