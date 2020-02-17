ALTER TABLE public.extension
    ADD download_count integer;

UPDATE public.extension
    SET download_count = 0;

ALTER TABLE public.extension
    ALTER COLUMN download_count SET NOT NULL;
