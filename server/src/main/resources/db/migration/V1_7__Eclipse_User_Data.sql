ALTER TABLE public.user_data
    ADD eclipse_data character varying(4096);

ALTER TABLE public.user_data
    RENAME COLUMN provider_id TO auth_id;
