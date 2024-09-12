ALTER TABLE public.extension ADD COLUMN deprecated BOOLEAN;
ALTER TABLE public.extension ADD COLUMN replacement_id BIGINT;
ALTER TABLE public.extension ADD COLUMN downloadable BOOLEAN;

UPDATE public.extension SET deprecated = FALSE;
UPDATE public.extension SET downloadable = TRUE;

ALTER TABLE public.extension ALTER COLUMN deprecated SET NOT NULL;
ALTER TABLE public.extension ALTER COLUMN downloadable SET NOT NULL;

ALTER TABLE public.extension ADD CONSTRAINT extension_replacement_id_fkey
FOREIGN KEY (replacement_id) REFERENCES public.extension(id);