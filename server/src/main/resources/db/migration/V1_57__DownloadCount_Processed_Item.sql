CREATE TABLE IF NOT EXISTS public.download_count_processed_item (id BIGINT NOT NULL,
                                                                 name CHARACTER VARYING(255) NOT NULL,
                                                                 storage_type CHARACTER VARYING(32),
                                                                 processed_on TIMESTAMP WITHOUT TIME ZONE,
                                                                 execution_time INT,
                                                                 success BOOLEAN NOT NULL
);

ALTER TABLE ONLY public.download_count_processed_item
    ADD CONSTRAINT download_count_processed_item_pkey PRIMARY KEY (id);

CREATE INDEX download_count_processed_item_storage_type ON download_count_processed_item (storage_type);

CREATE SEQUENCE download_count_processed_item_seq INCREMENT 50 OWNED BY public.download_count_processed_item.id;
SELECT SETVAL('download_count_processed_item_seq', (SELECT COALESCE(MAX(id), 1) FROM download_count_processed_item)::BIGINT);

DROP TABLE IF EXISTS public.azure_download_count_processed_item;

DELETE FROM public.jobrunr_recurring_jobs where id = 'update-download-counts';
