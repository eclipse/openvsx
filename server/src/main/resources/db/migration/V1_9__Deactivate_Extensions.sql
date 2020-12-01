ALTER TABLE public.extension
    ADD active boolean NOT NULL;

UPDATE public.extension
    SET active = true;

ALTER TABLE public.extension_version
    ADD active boolean NOT NULL;

UPDATE public.extension_version
    SET active = true;
