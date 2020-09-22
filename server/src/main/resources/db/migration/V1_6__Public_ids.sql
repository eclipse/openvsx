ALTER TABLE public.extension
    ADD public_id character varying(128),
    ADD CONSTRAINT unique_extension_public_id UNIQUE (public_id);

ALTER TABLE public.namespace
    ADD public_id character varying(128),
    ADD CONSTRAINT unique_namespace_public_id UNIQUE (public_id);
