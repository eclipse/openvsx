ALTER TABLE public.extension
    ADD active boolean;

UPDATE public.extension
    SET active = true;

ALTER TABLE public.extension
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE public.extension_version
    ADD active boolean;

UPDATE public.extension_version
    SET active = true;

ALTER TABLE public.extension_version
    ALTER COLUMN active SET NOT NULL;
