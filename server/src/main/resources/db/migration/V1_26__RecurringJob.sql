CREATE TABLE public.recurring_job (
    id BIGINT NOT NULL,
    job_id CHARACTER VARYING(255) NOT NULL,
    prefix CHARACTER VARYING(255) NOT NULL,
    schedule CHARACTER VARYING(255) NOT NULL
);

ALTER TABLE ONLY public.recurring_job ADD CONSTRAINT recurring_job_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX recurring_job_prefix ON recurring_job (prefix);