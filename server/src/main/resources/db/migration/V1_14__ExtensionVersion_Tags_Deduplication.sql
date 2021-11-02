ALTER TABLE extension_version ALTER COLUMN tags TYPE CHARACTER VARYING(2560);

CREATE TABLE public.extension_version_tag (
  extension_version_id BIGINT NOT NULL,
  tag CHARACTER VARYING(255) NOT NULL,
  tag_lower CHARACTER VARYING(255),
  tag_index_in_tags INT
);

ALTER TABLE public.extension_version_tag ADD CONSTRAINT extension_version_tag_extension_version_id_fkey
FOREIGN KEY (extension_version_id) REFERENCES public.extension_version(id);

INSERT INTO public.extension_version_tag(extension_version_id, tag)
SELECT id, UNNEST(STRING_TO_ARRAY(tags, ',')) tag FROM extension_version;

UPDATE public.extension_version_tag SET tag_lower = LOWER(tag);

UPDATE public.extension_version_tag t
SET tag_index_in_tags = ARRAY_POSITION(STRING_TO_ARRAY(ev.tags, ','), t.tag::text)
FROM public.extension_version ev
WHERE t.extension_version_id = ev.id;

UPDATE public.extension_version ev
SET tags = agg.tags
FROM (
	SELECT ot.extension_version_id, STRING_AGG(ot.tag, ',') tags
	FROM (
		SELECT t.extension_version_id, t.tag
		FROM public.extension_version_tag t
		JOIN (
			SELECT extension_version_id, tag_lower, MIN(tag_index_in_tags) min_index
			FROM public.extension_version_tag
			GROUP BY extension_version_id, tag_lower
		) gt ON gt.extension_version_id = t.extension_version_id AND gt.min_index = t.tag_index_in_tags
		ORDER BY t.extension_version_id, t.tag_lower COLLATE "ucs_basic"
	) ot
	GROUP BY ot.extension_version_id
) agg
WHERE ev.id = agg.extension_version_id;

DROP TABLE public.extension_version_tag;