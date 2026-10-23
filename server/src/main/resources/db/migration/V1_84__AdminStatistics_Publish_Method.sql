CREATE TABLE public.admin_statistics_extension_versions_by_publish_method(
    admin_statistics_id BIGINT NOT NULL,
    publish_method CHARACTER VARYING(32) NOT NULL,
    extension_versions INT NOT NULL
);

ALTER TABLE ONLY public.admin_statistics_extension_versions_by_publish_method
    ADD CONSTRAINT admin_statistics_extension_versions_by_publish_method_fkey FOREIGN KEY (admin_statistics_id) REFERENCES admin_statistics(id);

-- Unlike the snapshot figures this one derives from publish timestamps, so already archived months can be backfilled.
INSERT INTO public.admin_statistics_extension_versions_by_publish_method(admin_statistics_id, publish_method, extension_versions)
SELECT s.id, COALESCE(ev.published_with_tt, 'NONE'), COUNT(DISTINCT (ev.extension_id, ev.version))
FROM public.admin_statistics s
JOIN public.extension_version ev
    ON ev.timestamp >= make_timestamp(s.year, s.month, 1, 0, 0, 0)
    AND ev.timestamp < make_timestamp(s.year, s.month, 1, 0, 0, 0) + INTERVAL '1 month'
GROUP BY s.id, COALESCE(ev.published_with_tt, 'NONE');
