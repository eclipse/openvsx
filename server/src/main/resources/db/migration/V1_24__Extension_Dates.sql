ALTER TABLE extension ADD COLUMN published_date TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE extension ADD COLUMN last_updated_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE extension e
SET published_date = min_timestamp, last_updated_date = max_timestamp
FROM(
	SELECT extension_id, MIN(timestamp) min_timestamp, MAX(timestamp) max_timestamp
	FROM extension_version
	GROUP BY extension_id
) ev
WHERE e.id = ev.extension_id;
