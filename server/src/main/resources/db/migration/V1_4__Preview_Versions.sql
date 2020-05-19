ALTER TABLE public.extension
    ADD preview_id bigint;

ALTER TABLE ONLY public.extension
    ADD CONSTRAINT extension_preview_fkey FOREIGN KEY (preview_id) REFERENCES public.extension_version(id);
