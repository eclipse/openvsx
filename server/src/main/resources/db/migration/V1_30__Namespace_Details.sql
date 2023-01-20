-- create namespace_social_links table
CREATE TABLE public.namespace_social_links(
    namespace_id BIGINT NOT NULL,
    provider CHARACTER VARYING(255) NOT NULL,
    social_link CHARACTER VARYING(255) NOT NULL
);

ALTER TABLE ONLY public.namespace_social_links
    ADD CONSTRAINT namespace_social_links_fkey FOREIGN KEY (namespace_id) REFERENCES public.namespace(id);

-- update namespace table
ALTER TABLE public.namespace ADD COLUMN display_name CHARACTER VARYING(32);
ALTER TABLE public.namespace ADD COLUMN description CHARACTER VARYING(255);
ALTER TABLE public.namespace ADD COLUMN website CHARACTER VARYING(255);
ALTER TABLE public.namespace ADD COLUMN support_link CHARACTER VARYING(255);
ALTER TABLE public.namespace ADD COLUMN logo_name CHARACTER VARYING(255);
ALTER TABLE public.namespace ADD COLUMN logo_bytes BYTEA;
ALTER TABLE public.namespace ADD COLUMN logo_storage_type CHARACTER VARYING(32);