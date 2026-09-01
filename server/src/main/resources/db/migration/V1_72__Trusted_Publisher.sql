-- trusted_publisher new table

CREATE SEQUENCE IF NOT EXISTS trusted_publisher_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS public.trusted_publisher
(
    id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('trusted_publisher_seq'),
    -- A registration only means anything for the extension it points at, so it goes when that extension
    -- is purged. Cascading rather than restricting, because purging is an administrative action that has
    -- to succeed: without this, an extension that ever had a trusted publisher could never be purged.
    extension_id BIGINT NOT NULL REFERENCES public.extension(id) ON DELETE CASCADE,
    provider CHARACTER VARYING(32) NOT NULL,
    registration JSONB NOT NULL,
    claims JSONB NOT NULL,
    -- Only a namespace owner may register a trusted publisher, so a registration cannot outlive its
    -- author any more than it outlives their ownership. Deleting a user already revokes their
    -- registrations by way of their memberships; this is the constraint saying the same thing, and keeps
    -- this reference consistent with the others in this migration.
    created_by BIGINT NOT NULL REFERENCES public.user_data(id) ON DELETE CASCADE,
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
    -- optional; the extension that the token is scoped to. Deleted with it rather than detached from
    -- it: AccessTokenService.getScope treats a token with neither scope set as unrestricted, so setting
    -- this to NULL would silently widen a scoped token instead of retiring it. A token scoped to an
    -- extension that no longer exists can authorize nothing anyway.
    ADD COLUMN IF NOT EXISTS scope_extension_id BIGINT REFERENCES public.extension(id) ON DELETE CASCADE,
    -- optional; the namespace that the token is scoped to. Cascaded for the same reason.
    ADD COLUMN IF NOT EXISTS scope_namespace_id BIGINT REFERENCES public.namespace(id) ON DELETE CASCADE,
    -- optional; the trusted publisher that the token was created for. Deleting a registration retires the
    -- tokens issued under it: they may only ever publish the one extension it was made for, so once it is
    -- gone they can authorize nothing.
    ADD COLUMN IF NOT EXISTS trusted_publisher_id BIGINT REFERENCES public.trusted_publisher(id) ON DELETE CASCADE,
    -- optional; the OIDC claims the token was exchanged for, carried from the exchange to the publish that
    -- uses it. This row does not keep them - a one-time token is deleted as it is used - it hands them to
    -- the version being published, see extension_version.published_provenance below.
    ADD COLUMN IF NOT EXISTS claims JSONB;

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

-- detaching extension_version from personal_access_token

-- Who published a version and what token type was used, recorded directly instead of being reached through the token
-- that was used for the upload. A token is a credential with its own lifecycle -- it can be revoked, expire, or (in
-- future) be deleted -- while the authorship of a version is permanent and has to outlive all three.
-- Every read path that asks "who published this?" uses this column from now on.
ALTER TABLE public.extension_version
    ADD COLUMN published_by_id BIGINT,
    ADD COLUMN published_with_tt CHARACTER VARYING(32),
    -- The OIDC identity that produced this version, for versions published through trusted publishing:
    -- the immutable repository and owner ids and the workflow reference including the ref it ran on, as
    -- the provider asserted them at the exchange. Copied rather than referenced for the usual reason -
    -- the token is deleted as it is used, and the registration can be revoked - and this link from a
    -- published artifact back to the workflow run is most of what trusted publishing buys over a token
    -- somebody pasted into CI.
    ADD COLUMN published_provenance JSONB;

-- Backfill from the only place the answer exists today. Rows whose published_with_id is already NULL
-- have nothing to derive it from and stay NULL, which is why this column is deliberately left nullable:
-- every read path has always had to treat an unknown publisher as "not verified" / omitted from JSON,
-- and they keep doing exactly that.
UPDATE public.extension_version ev
    SET published_by_id = pat.user_data,
        published_with_tt = pat.type
    FROM public.personal_access_token pat
    WHERE pat.id = ev.published_with_id;

ALTER TABLE public.extension_version
    ADD CONSTRAINT extension_version_published_by_id_fkey FOREIGN KEY (published_by_id) REFERENCES public.user_data(id);

-- Publisher-keyed lookups (a user's own versions, the publisher compliance check, admin statistics)
-- now filter on this column instead of joining through the token.
CREATE INDEX extension_version__published_by_id__idx ON public.extension_version (published_by_id);

-- published_with_id keeps recording which credential was used, but as best-effort provenance rather
-- than a hard dependency: it answers "what did this token publish" after a leak, while authorship is
-- recorded permanently in published_by_id above. Deleting a token clears the reference instead of being
-- refused by the database, so a one-time token used up, or a forgotten user's tokens, simply leave it
-- null. The base migration left this constraint named by Hibernate; drop it by that generated name and
-- recreate under the naming convention used everywhere else on this table.
ALTER TABLE public.extension_version
    DROP CONSTRAINT fk70khj8pm0vacasuiiaq0w0r80;
ALTER TABLE public.extension_version
    ADD CONSTRAINT extension_version_published_with_id_fkey
        FOREIGN KEY (published_with_id) REFERENCES public.personal_access_token(id) ON DELETE SET NULL;


-- and now we can delete all inactive one-time-usable personal access tokens
DELETE FROM public.personal_access_token pat
    WHERE pat.active = false AND pat.type != 'LLT';