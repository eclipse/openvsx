ALTER TABLE extension ADD COLUMN review_count BIGINT;

UPDATE extension e
SET review_count = reviews
FROM (
    SELECT extension_id, COUNT(id) reviews
    FROM extension_review
    WHERE active = TRUE
    GROUP BY extension_id
) r
WHERE e.id = r.extension_id;