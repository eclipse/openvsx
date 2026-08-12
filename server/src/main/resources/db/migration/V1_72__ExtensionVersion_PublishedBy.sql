-- Who published a version, recorded directly instead of being reached through the token that was used
-- for the upload. A token is a credential with its own lifecycle -- it can be revoked, expire, or (in
-- future) be deleted -- while the authorship of a version is permanent and has to outlive all three.
-- Every read path that asks "who published this?" uses this column from now on.
ALTER TABLE public.extension_version ADD COLUMN published_by_id BIGINT;

-- Backfill from the only place the answer exists today. Rows whose published_with_id is already NULL
-- have nothing to derive it from and stay NULL, which is why this column is deliberately left nullable:
-- every read path has always had to treat an unknown publisher as "not verified" / omitted from JSON,
-- and they keep doing exactly that.
UPDATE public.extension_version ev
SET published_by_id = pat.user_data
FROM public.personal_access_token pat
WHERE pat.id = ev.published_with_id;

ALTER TABLE public.extension_version ADD CONSTRAINT extension_version_published_by_id_fkey
FOREIGN KEY (published_by_id) REFERENCES public.user_data(id);

-- Publisher-keyed lookups (a user's own versions, the publisher compliance check, admin statistics)
-- now filter on this column instead of joining through the token.
CREATE INDEX extension_version__published_by_id__idx ON public.extension_version (published_by_id);

-- published_with_id keeps recording which credential was used, but as best-effort provenance rather
-- than a hard dependency: deleting a token in future clears the reference instead of being refused by
-- the database. The base migration left this constraint named by Hibernate; drop it by that generated
-- name and recreate under the naming convention used everywhere else on this table.
ALTER TABLE public.extension_version DROP CONSTRAINT fk70khj8pm0vacasuiiaq0w0r80;
ALTER TABLE public.extension_version ADD CONSTRAINT extension_version_published_with_id_fkey
FOREIGN KEY (published_with_id) REFERENCES public.personal_access_token(id) ON DELETE SET NULL;
