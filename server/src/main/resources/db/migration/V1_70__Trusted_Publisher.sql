-- trusted_publisher table

CREATE SEQUENCE IF NOT EXISTS trusted_publisher_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.trusted_publisher
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('trusted_publisher_seq'),
    namespace BIGINT NOT NULL REFERENCES public.namespace(id),
    extension_name CHARACTER VARYING(255),
    provider CHARACTER VARYING(32) NOT NULL,
    registration JSONB NOT NULL,
    claims JSONB NOT NULL,
    created_by BIGINT NOT NULL REFERENCES public.user_data(id),
    created_timestamp TIMESTAMP without time zone NOT NULL
);

CREATE INDEX IF NOT EXISTS trusted_publisher_namespace_idx ON public.trusted_publisher (namespace, extension_name);
