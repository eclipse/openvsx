-- Extension versions are immutable: a "deleted" version is soft-deleted (marked removed) instead of
-- being physically removed, so its identity stays reserved and can never be republished.
ALTER TABLE public.extension_version ADD COLUMN removed BOOLEAN;
ALTER TABLE public.extension_version ADD COLUMN removed_timestamp TIMESTAMP;
ALTER TABLE public.extension_version ADD COLUMN removed_by_id BIGINT;

UPDATE public.extension_version SET removed = FALSE;

ALTER TABLE public.extension_version ALTER COLUMN removed SET NOT NULL;

ALTER TABLE public.extension_version ADD CONSTRAINT extension_version_removed_by_id_fkey
FOREIGN KEY (removed_by_id) REFERENCES public.user_data(id);
