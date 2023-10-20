-- make unique constraints deferred, so that the unique constraint is evaluated at transaction commit time
-- instead of validating immediately row-by-row for each insert or update statement.
ALTER TABLE public.extension
    DROP CONSTRAINT unique_extension_public_id,
    ADD CONSTRAINT unique_extension_public_id UNIQUE (public_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.namespace
    DROP CONSTRAINT unique_namespace_public_id,
    ADD CONSTRAINT unique_namespace_public_id UNIQUE (public_id) DEFERRABLE INITIALLY DEFERRED;