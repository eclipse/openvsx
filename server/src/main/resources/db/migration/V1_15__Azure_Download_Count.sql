CREATE TABLE public.azure_download_count_processed_item (
    id BIGINT NOT NULL,
    name CHARACTER VARYING(255) NOT NULL,
    processed_on TIMESTAMP WITHOUT TIME ZONE,
    execution_time INT,
    success BOOLEAN NOT NULL
);

ALTER TABLE ONLY public.azure_download_count_processed_item
    ADD CONSTRAINT azure_download_count_processed_item_pkey PRIMARY KEY (id);

CREATE TABLE public.shedlock(
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

ALTER TABLE ONLY public.shedlock ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);
