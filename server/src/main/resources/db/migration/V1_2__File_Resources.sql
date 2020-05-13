CREATE TABLE public.file_resource (
    id bigint NOT NULL,
    type character varying(32),
    content bytea,
    extension_id bigint
);

ALTER TABLE ONLY public.file_resource
    ADD CONSTRAINT file_resource_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.file_resource
    ADD CONSTRAINT file_resource_extension_fkey FOREIGN KEY (extension_id) REFERENCES public.extension_version(id);

CREATE SEQUENCE file_resource_id_seq OWNED BY public.file_resource.id;

INSERT INTO public.file_resource (id, type, content, extension_id)
    SELECT nextval('file_resource_id_seq'), 'download', content, extension_id
    FROM public.extension_binary;

INSERT INTO public.file_resource (id, type, content, extension_id)
    SELECT nextval('file_resource_id_seq'), 'readme', content, extension_id
    FROM public.extension_readme;

INSERT INTO public.file_resource (id, type, content, extension_id)
    SELECT nextval('file_resource_id_seq'), 'license', content, extension_id
    FROM public.extension_license;

INSERT INTO public.file_resource (id, type, content, extension_id)
    SELECT nextval('file_resource_id_seq'), 'icon', content, extension_id
    FROM public.extension_icon;

DROP TABLE public.extension_binary;

DROP TABLE public.extension_readme;

DROP TABLE public.extension_license;

DROP TABLE public.extension_icon;
