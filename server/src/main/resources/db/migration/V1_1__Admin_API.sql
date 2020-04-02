ALTER TABLE public.user_data
    ADD role character varying(32);

CREATE TABLE public.persisted_log (
    id bigint NOT NULL,
    "timestamp" timestamp without time zone,
    user_data bigint,
    message character varying(512)
);

ALTER TABLE ONLY public.persisted_log
    ADD CONSTRAINT persisted_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.persisted_log
    ADD CONSTRAINT persisted_log_user_data_fkey FOREIGN KEY (user_data) REFERENCES public.user_data(id);
