-- trusted_publisher table

CREATE SEQUENCE IF NOT EXISTS trusted_publisher_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.trusted_publisher
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('trusted_publisher_seq'),
    extension_id BIGINT NOT NULL REFERENCES public.extension(id),
    provider CHARACTER VARYING(32) NOT NULL,
    registration JSONB NOT NULL,
    claims JSONB NOT NULL,
    created_by BIGINT NOT NULL REFERENCES public.user_data(id),
    created_timestamp TIMESTAMP without time zone NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS trusted_publisher_extension_idx ON public.trusted_publisher (extension_id);

-- token.type

ALTER TABLE ONLY public.personal_access_token
    -- extend the value column to 128 characters
    ALTER COLUMN value TYPE CHARACTER VARYING(128),
    -- token version
    ADD COLUMN IF NOT EXISTS version SMALLINT,
    -- the type column LLT, OTT or TPT
    ADD COLUMN IF NOT EXISTS type CHARACTER VARYING(32),
    -- optional; the extension that the token is scoped to
    ADD COLUMN IF NOT EXISTS scope_extension_id BIGINT REFERENCES public.extension(id),
    -- optional; the namespace that the token is scoped to
    ADD COLUMN IF NOT EXISTS scope_namespace_id BIGINT REFERENCES public.namespace(id),
    -- optional; the trusted publisher that the token was created for (token must remain; registration may be deleted)
    ADD COLUMN IF NOT EXISTS trusted_publisher_id BIGINT REFERENCES public.trusted_publisher(id) ON DELETE SET NULL;

-- set OTT based on description (is hardwired in codebase)
UPDATE public.personal_access_token
SET type = 'OTT'
WHERE personal_access_token.description = 'One time use publish token';

-- set LLT for all other tokens
UPDATE public.personal_access_token
SET type = 'LLT'
WHERE personal_access_token.type IS NULL;

-- set version = 0 on all
UPDATE public.personal_access_token
SET version = 0;

-- type is required for all tokens
ALTER TABLE ONLY public.personal_access_token
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN type SET NOT NULL;
