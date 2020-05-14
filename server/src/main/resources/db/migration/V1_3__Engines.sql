CREATE TABLE public.extension_version_engines (
    extension_version_id bigint NOT NULL,
    engines character varying(255)
);

ALTER TABLE ONLY public.extension_version_engines
    ADD CONSTRAINT engines_extension_version_fkey FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);
