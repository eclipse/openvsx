ALTER TABLE public.extension_review
    ADD active boolean;

UPDATE public.extension_review
    SET active = true;

ALTER TABLE public.extension_review
    ALTER COLUMN active SET NOT NULL;
